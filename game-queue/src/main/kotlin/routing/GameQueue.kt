package routing

import SearchingForGameConnection
import io.github.kroune.logging.logger
import sendSerializedEvent
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import data.dao.UsersDataServiceI
import io.github.kroune.routing.requireValidJwtToken

fun Route.gameQueueRouting() {
    webSocket("/search-for-game") {
        val jwtToken = requireValidJwtToken() ?: return@webSocket

        val usersRepository by inject<UsersDataServiceI>()
        val userId = usersRepository.getIdByJwtToken(jwtToken)!!
        val expectedWaitingTime = MutableStateFlow<Long?>(null)
        val handler = CoroutineExceptionHandler { _, error ->
            logger.atInfo {
                message = "error while sending a move"
                cause = error
            }
        }
        val waitingTimeJob = launch(handler) {
            expectedWaitingTime.collect {
                if (it != null)
                    sendSerializedEvent(data = it, metadata = "waiting_time")
            }
        }
        val onGameFound: suspend (Long) -> Unit = { data: Long ->
            sendSerializedEvent(data = data, metadata = "game_id")
            flush()
            waitingTimeJob.cancel()
            close()
        }
        SearchingForGame.addUser(
            userId,
            SearchingForGameConnection(expectedWaitingTime, onGameFound)
        )
        closeReason.await()
        SearchingForGame.removeUser(userId)
    }
}
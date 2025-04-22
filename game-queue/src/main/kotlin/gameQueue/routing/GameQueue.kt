package gameQueue.routing

import common.sendSerializedEvent
import gameQueue.controller.QueueController
import gameQueue.controller.SearchingForGameConnection
import common.logging.logger
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import user.data.dao.UsersDataServiceI
import user.routing.requireValidJwtToken

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
        val queueController by inject<QueueController>()
        queueController.addUser(
            userId,
            SearchingForGameConnection(expectedWaitingTime, onGameFound)
        )
        closeReason.await()
        queueController.removeUser(userId)
    }
}
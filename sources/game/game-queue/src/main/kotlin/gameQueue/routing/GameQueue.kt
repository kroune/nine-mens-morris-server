package gameQueue.routing

import common.logging.logger
import common.sendSerializedEvent
import gameQueue.service.QueueService
import gameQueue.service.SearchingForGameConnection
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import userApi.controller.requireValidJwtToken
import userApi.data.dao.UsersDataServiceI

fun Route.gameQueueRouting() {
    webSocket("/search-for-game") {
        val jwtToken = requireValidJwtToken() ?: return@webSocket

        val usersRepository by inject<UsersDataServiceI>()
        val userId = usersRepository.getIdByJwtToken(jwtToken)!!
        val handler = CoroutineExceptionHandler { _, error ->
            logger.atInfo {
                message = "error while sending a waiting time"
                cause = error
            }
        }
        val expectedWaitingTime = MutableStateFlow<Long?>(null)
        val waitingTimeJob = launch(handler) {
            expectedWaitingTime.collect {
                if (it != null)
                    sendSerializedEvent(data = it, metadata = "waiting_time")
            }
        }
        val queueService by inject<QueueService>()
        queueService.addUser(
            userId,
            SearchingForGameConnection(expectedWaitingTime) { data ->
                sendSerializedEvent(data = data, metadata = "game_id")
                flush()
                waitingTimeJob.cancel()
                close()
            }
        )
        closeReason.await()
        queueService.removeUser(userId)
    }
}
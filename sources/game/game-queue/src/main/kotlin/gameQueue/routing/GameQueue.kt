package gameQueue.routing

import common.fireAndForgetScope
import common.sendSerializedEvent
import gameQueue.service.QueueService
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import userApi.controller.requireValidJwtToken
import userApi.data.dao.UsersDataServiceI

fun Route.gameQueueRouting() {
    webSocket("/search-for-game") {
        val jwtToken = requireValidJwtToken() ?: return@webSocket

        val usersRepository by inject<UsersDataServiceI>()
        val userId = usersRepository.getIdByJwtToken(jwtToken)!!
        val queueService by inject<QueueService>()
        val expectedWaitingTime = queueService.addUser(
            userId,
            onGameFound = { data ->
                sendSerializedEvent(data = data, metadata = "game_id")
                flush()
                close()
            },
        )
        closeReason.invokeOnCompletion {
            fireAndForgetScope.launch {
                queueService.removeUser(userId)
            }
        }
        for (waitingTime in expectedWaitingTime) {
            sendSerializedEvent(data = waitingTime, metadata = "waiting_time")
        }
    }
}
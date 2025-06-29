package gameQueue

import common.commonModule
import common.receiveDeserializedServerEvent
import commonTests.di.commonTestModule
import database.di.databaseModule
import gameMain.di.gameMainModules
import gameQueue.di.queueModules
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import user.di.usersModules
import kotlin.time.Duration.Companion.seconds

fun ApplicationTestBuilder.getGameId(jwtToken: String): Pair<Deferred<Long>, Channel<Long>> {
    val channel = Channel<Long>(1000)
    return CoroutineScope(Dispatchers.IO).async {
        val client2 = createClient {
            install(HttpRequestRetry) {
                // retry on timeout
                retryIf(maxRetries = 5) { _, response ->
                    response.status.value == 408
                }
                retryOnExceptionIf(maxRetries = 5) { _, exception ->
                    exception is HttpRequestTimeoutException
                }
                exponentialDelay()
            }
            install(HttpTimeout) {
                this.requestTimeoutMillis = 10 * 1000
                this.socketTimeoutMillis = 30 * 60 * 1000
                this.connectTimeoutMillis = 10 * 1000
            }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
                pingInterval = 3.seconds
            }
        }

        var gameId: Long? = null
        println("starting ws")
        client2.ws(urlString = "/search-for-game", request = {
            url {
                parameter("jwtToken", jwtToken)
            }
        }) {
            while (true) {
                println("waiting for info")
                val info = receiveDeserializedServerEvent<Long, String>()
                println(info)
                when (info.second) {
                    "game_id" -> {
                        gameId = info.first
                        channel.close()
                        close()
                        println("game id = ${info.first}")
                        return@ws
                    }

                    "waiting_time" -> {
                        channel.send(info.first)
                    }

                    else -> {
                        println("WTF HAPPENED $info")
                    }
                }
            }
        }
        client2.close()
        return@async gameId!!
    } to channel
}

fun Application.startTestDI() {
    install(Koin) {
        modules(
            commonModule,
            commonTestModule,
            databaseModule,
            gameMainModules,
            usersModules,
            queueModules
        )
    }
}

import io.github.kroune.common.receiveDeserializedServerEvent
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

fun ApplicationTestBuilder.getGameId(jwtToken: String): Deferred<Long> {
    return CoroutineScope(Dispatchers.IO).async {

        val client2 = createClient {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
                pingInterval = 3.seconds
            }
        }
        var gameId: Long? = null
        client2.ws(urlString = "/search-for-game", request = {
            url {
                parameter("jwtToken", jwtToken)
            }
        }) {
            while (true) {
                val info = receiveDeserializedServerEvent<Long, String>()
                if (info.second == "game_id") {
                    gameId = info.first
                    println("game id = ${info.first}")
                    break
                }
            }
        }
        return@async gameId!!
    }
}
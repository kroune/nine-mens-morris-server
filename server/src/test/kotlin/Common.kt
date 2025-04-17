import io.github.kroune.TestKafka
import io.github.kroune.di.koinModules
import ConfigurationLoader.ConfigMember
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.time.Duration.Companion.seconds

fun ApplicationTestBuilder.getGameId(jwtToken: String): Pair<Deferred<Long>, Channel<Long>> {
    val channel = Channel<Long>(1000)
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
                when (info.second) {
                    "game_id" -> {
                        gameId = info.first
                        channel.close()
                        println("game id = ${info.first}")
                        break
                    }
                    "waiting_time" -> {
                        channel.send(info.first)
                    }
                    else -> {
                        info.second
                        info.first
                    }
                }
            }
        }
        return@async gameId!!
    } to channel
}

fun Application.startTestDI() {
    install(Koin) {
        modules(
            containersModules,
            koinModules,
        )
    }
}

val testConfig = ConfigMember(
    serverConfig = ConfigurationLoader.ServerConfig("0.0.0.0", 8080),
    rateLimitConfig = ConfigurationLoader.RateLimitConfig(1000, 10.seconds, 1000),
    webSocketConfig = ConfigurationLoader.WebSocketConfig(3.seconds, 10.seconds),
    encryptionToken = "thisIsEncryptionTokenI_WILL_FUCKING_KILL_YOU_IF_YOU_USE_THIS_IN_PROD",
    fileConfig = ConfigurationLoader.FileConfig(256),
    kafkaConfig = ConfigurationLoader.KafkaConfig(TestKafka.kafka.bootstrapServers),
    grafanaConfig = ConfigurationLoader.GrafanaConfig("", ""),
    gameConfig = ConfigurationLoader.GameConfig(
        timeForMove = 30_000,
        maxBucketNumber = 100,
        maxRatingDifference = 50,
        bucketSize = Int.MAX_VALUE,
        maxRating = 1000,
        delayBeforeRecheckingBucket = 5_000,
        minTimeBeforePairingWithBot = 30_000,
        maxTimeBeforePairingWithBot = 35_000
    ),
    serviceLocator = ConfigurationLoader.ServiceLocator(ConfigurationLoader.PostgresConfig("", "", ""))
)

val containersModules = module {
    single { testConfig }
}
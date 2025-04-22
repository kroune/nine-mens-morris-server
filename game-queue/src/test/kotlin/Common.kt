import common.ConfigurationLoader
import common.ConfigurationLoader.ConfigMember
import common.commonPlugins
import common.receiveDeserializedServerEvent
import gameMain.di.gameMainModules
import gameQueue.di.queueModules
import commonTests.TestDatabase
import commonTests.TestKafka
import user.di.usersModules
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.websocket.close
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
                        close()
                        break
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
            containersModules,
            gameMainModules,
            commonPlugins,
            usersModules,
            queueModules
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
    serviceLocator = with (TestDatabase.pgContainer) {
        ConfigurationLoader.ServiceLocator(ConfigurationLoader.PostgresConfig(this.jdbcUrl, this.username, this.password))
    }
)

val containersModules = module {
    single { testConfig }
}
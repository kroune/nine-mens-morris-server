import common.ConfigurationLoader
import common.ConfigurationLoader.ConfigMember
import common.commonPlugins
import commonTests.TestDatabase
import commonTests.TestKafka
import user.di.usersModules
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.time.Duration.Companion.seconds

fun Application.startTestDI() {
    install(Koin) {
        modules(
            commonPlugins,
            containersModules,
            usersModules,
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
package commonTests.di

import common.ConfigurationLoader
import common.ConfigurationLoader.ConfigMember
import commonTests.TestKafka
import org.koin.dsl.module
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.time.Duration.Companion.seconds


val testConfig
    get() = ConfigMember(
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
        databasesConfig = run {
            val pg = PostgreSQLContainer<Nothing>("postgres:17").apply {
                withDatabaseName("test-db")
                withUsername("test-user")
                withPassword("test-password")
                start() // Start the container
            }
            with(pg) {
                ConfigurationLoader.DatabasesConfig(
                    userData = ConfigurationLoader.PostgresConfig(
                        this.jdbcUrl,
                        this.username,
                        this.password
                    ),
                    botsData = ConfigurationLoader.PostgresConfig(
                        this.jdbcUrl,
                        this.username,
                        this.password
                    )
                )
            }
        }
    )

val commonTestModule = module {
    single { testConfig }
}
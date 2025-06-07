package common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration

object ConfigurationLoader {
    @Serializable
    data class ConfigMember(
        val serverConfig: ServerConfig,
        val rateLimitConfig: RateLimitConfig,
        val webSocketConfig: WebSocketConfig,
        val encryptionToken: String,
        val fileConfig: FileConfig,
        val kafkaConfig: KafkaConfig,
        val grafanaConfig: GrafanaConfig,
        val gameConfig: GameConfig,
        val databasesConfig: DatabasesConfig,
        val versionUpdateConfig: VersionUpdateConfig
    )

    @Serializable
    data class VersionUpdateConfig(
        val tokens: Map<String, Boolean>
    )

    @Serializable
    class DatabasesConfig(
        val userData: PostgresConfig,
        val botsData: PostgresConfig,
        val versionData: PostgresConfig
    )

    @Serializable
    data class KafkaConfig(
        val url: String
    )

    @Serializable
    class GrafanaConfig(
        val nameForScrape: String,
        val passwordForScrape: String,
    )

    @Serializable
    class PostgresConfig(
        val url: String,
        val username: String,
        val password: String
    )

    @Serializable
    class GameConfig(
        val timeForMove: Long,
        val maxBucketNumber: Int = 100,
        val maxRatingDifference: Int = 200,
        val bucketSize: Int = 50,
        val maxRating: Int = Int.MAX_VALUE,
        val delayBeforeRecheckingBucket: Long,
        val minTimeBeforePairingWithBot: Long,
        val maxTimeBeforePairingWithBot: Long
    )

    @Serializable
    class ServerConfig(
        val host: String,
        val port: Int,
    )

    @Serializable
    class WebSocketConfig(
        val pingPeriod: Duration,
        val timeout: Duration
    )

    @Serializable
    class RateLimitConfig(
        val rateLimit: Int,
        val refillSpeed: Duration,
        val initialSize: Int
    )

    @Serializable
    class FileConfig(
        val profilePictureMaxSize: Int
    )

    private fun loadConfig(): ConfigMember {
        val configDirectory = System.getenv("CONFIG_PATH") ?: "/etc/game-server/config.json"
        val config = File(configDirectory).readText()
        return Json.decodeFromString<ConfigMember>(config)
    }

    val currentConfig: ConfigMember = loadConfig()
}
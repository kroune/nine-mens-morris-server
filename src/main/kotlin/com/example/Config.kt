package com.example

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.io.File

val currentConfig: Config = run {
    val configFile = File("/etc/config/server-config.yaml")
    if (!configFile.exists()) {
        error("config file wasn't provided")
    }
    val config = Yaml.default.decodeFromString<Config>(configFile.readText())
    println("decoded config successfully")
    config
}

@Serializable
data class Config(
    val serverConfig: ServerConfig,
    val rateLimitConfig: RateLimitConfig,
    val webSocketConfig: WebSocketConfig,
    val encryptionToken: String,
    val fileConfig: FileConfig,
    val currentLogPriority: LogPriority,
    val gameConfig: GameConfig
)

@Serializable
class GameConfig(
    val timeForMove: Long,
    val bucketSize: Long,
    val delayBeforeRecheckingBucket: Long
)

@Serializable
class ServerConfig(
    val host: String,
    val port: Int,
)

@Serializable
class WebSocketConfig(
    val pingPeriod: kotlin.time.Duration,
    val timeout: kotlin.time.Duration
)

@Serializable
class RateLimitConfig(
    val rateLimit: Int,
    val refillSpeed: kotlin.time.Duration
)

@Serializable
class FileConfig(
    val gameLogsPath: String,
    val dataDir: String,
    val botsDataDir: String
)
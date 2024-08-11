package com.example

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.io.File

val currentConfig: Config = run {
    val configDirList = listOf("server-config.yaml", "/etc/config/server-config.yaml")
    configDirList.forEach {
        val file = File(it)
        if (file.exists()) {
            // we found a working file
            return@run Yaml.default.decodeFromString<Config>(file.readText())
        }
    }
    error("config file wasn't provided")
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
/*
 * This file is part of nine-mens-morris-server (https://github.com/kroune/nine-mens-morris-server)
 * Copyright (C) 2024-2024  kroune
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact: kr0ne@tuta.io
 */
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
    val gameConfig: GameConfig,
    val isInKuber: Boolean = true
)

@Serializable
class GameConfig(
    val timeForMove: Long,
    val maxBucketNumber: Int = 100,
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
    val profilePictureMaxSize: Int
)
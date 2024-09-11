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

import com.example.data.botsRepository
import com.example.data.gamesRepository
import com.example.data.queueRepository
import com.example.data.usersRepository
import com.example.routing.auth.accountRouting
import com.example.routing.game.gameRouting
import com.example.routing.misc.miscRouting
import com.example.routing.userInfo.userInfoRouting
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import kotlin.time.toJavaDuration

fun main() {
    log("starting server", LogPriority.Info)
    log("printing server config", LogPriority.Info)
    log(json.encodeToString<Config>(currentConfig), LogPriority.Info)
    currentConfig
    val serverConfig = currentConfig.serverConfig
    embeddedServer(
        Netty,
        port = serverConfig.port,
        host = serverConfig.host,
        configure = {
            requestReadTimeoutSeconds = 15
            responseWriteTimeoutSeconds = 15
        },
        module = {
            module()
        }
    ).start(wait = true)
}

fun Application.module() {
    val localhost = "127.0.0.1:5432"
    val podDomain = "postgres-service.default.svc.cluster.local"
    val url = if (currentConfig.isInKuber) podDomain else localhost
    Database.connect(
        "jdbc:postgresql://$url/postgres",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "1234"
    )
    log("initializing users repository", LogPriority.Debug)
    usersRepository
    log("initializing games repository", LogPriority.Debug)
    gamesRepository
    log("initializing bots repository", LogPriority.Debug)
    botsRepository
    log("initializing queue repository", LogPriority.Debug)
    queueRepository
    log("applying configs", LogPriority.Debug)
}

fun Application.applyPlugins() {
    install(WebSockets) {
        val webSocketConfig = currentConfig.webSocketConfig
        pingPeriod = webSocketConfig.pingPeriod.toJavaDuration()
        timeout = webSocketConfig.timeout.toJavaDuration()
    }
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
            }
        )
    }
    install(RateLimit) {
        global {
            val rateLimitConfig = currentConfig.rateLimitConfig
            rateLimiter(limit = rateLimitConfig.rateLimit, refillPeriod = rateLimitConfig.refillSpeed)
        }
    }
}
fun Application.routing() {
    routing {
        log("initializing routing", LogPriority.Debug)
        miscRouting()
        route("/api/v1/user/") {
            userInfoRouting()
            gameRouting()
            accountRouting()
        }
    }
}

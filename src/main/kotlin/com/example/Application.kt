package com.example

import com.example.routing.auth.accountRouting
import com.example.routing.game.gameRouting
import com.example.routing.misc.miscRouting
import com.example.routing.userInfo.userInfoRouting
import com.kroune.nineMensMorrisLib.move.Movement
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import kotlin.time.toJavaDuration

fun main() {
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
    routing {
        miscRouting()
        route("/api/v1/user/") {
            userInfoRouting()
            gameRouting()
            accountRouting()
        }
    }
}

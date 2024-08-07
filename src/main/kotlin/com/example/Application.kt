package com.example

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
import kotlinx.serialization.json.Json
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        configure = {
            requestReadTimeoutSeconds = 15
            responseWriteTimeoutSeconds = 15
        },
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(3)
        timeout = Duration.ofSeconds(30)
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
            rateLimiter(limit = 30, refillPeriod = 60.seconds)
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

val SECRET_SERVER_TOKEN = System.getenv("SECRET_SERVER_TOKEN")
    ?: throw IllegalStateException("missing env variable, you need to set \"SECRET_SERVER_TOKEN\" to any string (used for encryption")

package com.example

import com.example.routing.api.userInfoRouting
import com.example.routing.auth.accountRouting
import com.example.routing.gameRouting
import com.example.routing.miscRouting
import com.kroune.NetworkResponse
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.pipeline.*
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
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
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

suspend fun PipelineContext<Unit, ApplicationCall>.notify(
    code: Int, message: String = ""
) {
    this.call.notify(code, message)
}

suspend fun ApplicationCall.notify(
    code: Int, message: String = ""
) {
    this.respondText { NetworkResponse(code, message).encode() }
    println(NetworkResponse(code, message).encode())
}

val SECRET_SERVER_TOKEN = System.getenv("SECRET_SERVER_TOKEN")
    ?: throw IllegalStateException("missing env variable, you need to set \"SECRET_SERVER_TOKEN\" to any string (used for encryption")

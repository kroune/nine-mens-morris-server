package com.example.routing

import com.kroune.NetworkResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, world!")
        }
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

suspend fun DefaultWebSocketSession.notify(
    code: Int, message: String = "", session: DefaultWebSocketSession = this
) {
    session.send(NetworkResponse(code, message).encode())
    println(NetworkResponse(code, message).encode())
}

val SECRET_SERVER_TOKEN = System.getenv("SECRET_SERVER_TOKEN") ?: throw IllegalStateException("missing env variable")

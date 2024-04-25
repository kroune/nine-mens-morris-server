package com.example.plugins

import com.example.users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/create-new-user") {
            val ip = this.call.request.origin.remoteAddress
            users.add(ip)
        }
        get("/create-new-game-{ip}") {
            val gameFriendIp = call.parameters["ip"] ?: return@get call.respondText(
                "Missing ip",
                status = HttpStatusCode.BadRequest
            )
            if (!users.contains(gameFriendIp)) {
                call.respondText(
                    "Your friend doesn't have an account",
                    status = HttpStatusCode.BadRequest
                )
            }
            val ip = this.call.request.origin.remoteAddress
            if (!users.contains(ip)) {
                call.respond("yYou need to create user first")
            }
        }
    }
}

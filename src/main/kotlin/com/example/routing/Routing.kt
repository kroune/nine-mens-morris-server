package com.example.routing

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.miscRouting() {
    get("/") {
        call.respondText("Hello, world!")
    }
    // used to calculate ping & check server status
    get("ping") {
        call.respondText("pong")
    }
}
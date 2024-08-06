package com.example.routing.misc

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.miscRoutingGET() {
    get("/") {
        call.respondText("Hello, world, this is nine mens morris server!")
    }
    get("ping") {
        call.respondText("pong")
    }
}

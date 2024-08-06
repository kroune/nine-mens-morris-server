package com.example.routing.auth

import com.example.routing.auth.get.accountRoutingGET
import io.ktor.server.routing.*

fun Route.accountRouting() {
    accountRoutingGET()
}

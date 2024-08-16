package com.example.routing.misc

import com.example.routing.misc.get.miscRoutingGET
import io.ktor.server.routing.*

fun Route.miscRouting() {
    miscRoutingGET()
}

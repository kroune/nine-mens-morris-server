package com.example.routing.game

import com.example.routing.game.get.gameRoutingGET
import com.example.routing.game.ws.gameRoutingWS
import io.ktor.server.routing.*

fun Route.gameRouting() {
    gameRoutingGET()
    gameRoutingWS()
}

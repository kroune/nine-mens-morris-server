package com.example.routing.game.get

import com.example.encryption.CustomJwtToken
import com.example.game.GamesDB
import com.example.requireValidJwtToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.gameRoutingGET() {
    /**
     * 400 = incorrect jwt token
     * null = user isn't playing currently
     * [Long] = game id
     *
     * 500 = internal server error
     */
    get("/is-playing") {
        requireValidJwtToken {
            return@get
        }
        val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
        val login = jwtToken.getLogin().getOrThrow()
        val gameId = GamesDB.gameId(login).onFailure {
            call.respond(HttpStatusCode.InternalServerError, "server error")
            return@get
        }
        call.respondText(gameId.getOrThrow().toString())
    }
}

package com.example.routing.game.get

import com.example.data.gamesRepository
import com.example.data.usersRepository
import com.example.responses.requireValidJwtToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        val jwtToken = call.parameters["jwtToken"]!!
        val userId = usersRepository.getIdByJwtToken(jwtToken) ?: run {
            call.respond(HttpStatusCode.InternalServerError, "server error")
            return@get
        }
        val gameId = gamesRepository.getGameIdByUserId(userId)
        val jsonText = Json.encodeToString<Long?>(gameId)
        call.respondText(jsonText)
    }
}

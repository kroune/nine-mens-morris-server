package com.example.routing

import com.example.CustomJwtToken
import com.example.game.Connection
import com.example.game.Games
import com.example.game.SearchingForGame
import com.example.users.Users
import com.example.users.Users.validateJwtToken
import com.kr8ne.mensMorris.PIECES_TO_FLY
import com.kr8ne.mensMorris.move.Movement
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import java.io.IOException

fun Route.gameRouting() {
    get("is-playing") {
        val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
        val checkResult = validateJwtToken(jwtToken)
        if (!checkResult) {
            notify(401, "incorrect jwt token")
            println("jwt token check failed")
            return@get
        }
        val gameId = Games.gameId(jwtToken)
        if (gameId.isFailure) {
            notify(409, "server error")
            return@get
        } else {
            notify(200, gameId.getOrThrow().toString())
        }
    }
    webSocket("/search-for-game") {
        val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
        val checkResult = validateJwtToken(jwtToken)
        if (!checkResult) {
            notify(401, "incorrect jwt token")
            println("jwt token check failed")
            close()
            return@webSocket
        }
        val thisConnection = Connection(jwtToken, this)
        SearchingForGame.addUser(thisConnection)
    }
    webSocket("/game") {
        val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
        val id = call.parameters["gameId"]!!.toLong()
        val game = Games.getGame(id)
        if (game == null) {
            notify(401, "incorrect game id")
            close()
            return@webSocket
        }
        if (!game.isParticipating(jwtToken)) {
            notify(401, "incorrect jwt token")
            close()
            return@webSocket
        }
        try {
            game.updateSession(jwtToken, this)
            val isGreen = when (jwtToken) {
                game.firstUser.jwtToken -> {
                    game.isFirstPlayerGreen
                }

                game.secondUser.jwtToken -> {
                    !game.isFirstPlayerGreen
                }

                else -> {
                    error("")
                }
            }
            send(isGreen.toString())
            println("sending isGreen status - $isGreen")
            // we send position to the new connection
            game.sendPosition(jwtToken, false)
            println("sending position status")
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach
                val move = try {
                    Json.decodeFromString<Movement>(frame.readText())
                } catch (e: Exception) {
                    null
                }
                if (move == null) {
                    notify(400, "error decoding movement ${frame.readText()}")
                    return@consumeEach
                }
                if (!game.isValidMove(move, jwtToken)) {
                    notify(400, "impossible move")
                    return@consumeEach
                }
                game.applyMove(move)
                // send new position to the enemy
                game.sendMove(jwtToken, move, true)
                // check if game has ended and then send this info
                if (game.hasEnded()) {
                    val firstUserRating = Users.getRatingById(game.firstUser.id().getOrThrow()).getOrThrow()
                    val secondUserRating = Users.getRatingById(game.secondUser.id().getOrThrow()).getOrThrow()
                    val greenLost = game.position.greenPiecesAmount < PIECES_TO_FLY
                    val firstUserLost = greenLost == game.isFirstPlayerGreen
                    val delta =
                        (10 + (if (firstUserLost) secondUserRating - firstUserRating else firstUserRating - secondUserRating) / 100).coerceIn(-50L..50L)
                    Users.updateRatingById(game.firstUser.id().getOrThrow(), if (firstUserLost) -delta else delta)
                    Users.updateRatingById(game.secondUser.id().getOrThrow(), if (firstUserLost) delta else -delta)
                    game.sendMove(jwtToken, Movement(null, null), true)
                    game.sendMove(jwtToken, Movement(null, null), false)
                    return@webSocket
                }
            }
        } catch (e: IOException) {
            println("user disconnected")
        }
    }
}

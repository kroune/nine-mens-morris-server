package com.example.routing

import com.example.*
import com.example.game.Connection
import com.example.game.Games
import com.example.game.SearchingForGame
import com.example.users.Users
import com.kr8ne.mensMorris.PIECES_TO_FLY
import com.kr8ne.mensMorris.move.Movement
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import java.io.IOException

fun Route.gameRouting() {
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
        val gameId = Games.gameId(jwtToken).onFailure {
            call.respond(HttpStatusCode.InternalServerError, "server error")
            return@get
        }
        call.respondText(gameId.getOrThrow().toString())
    }
    webSocket("/search-for-game") {
        requireValidJwtToken {
            return@webSocket
        }

        val jwtToken = call.parameters["jwtToken"]!!
        val thisConnection = Connection(jwtToken, this)
        SearchingForGame.addUser(thisConnection)
    }
    webSocket("/game") {
        requireValidJwtToken {
            return@webSocket
        }
        requireGameId {
            return@webSocket
        }
        val gameId = call.parameters["gameId"]!!.toLong()
        val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
        val game = Games.getGame(gameId)!!
        if (!game.isParticipating(jwtToken)) {
            jwtTokenIsNotValidForThisGame()
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
                    error("this jwt token doesn't belong to this game")
                }
            }
            send(isGreen.toString())
            log(gameId, "sending isGreen info - [$isGreen]")
            // we send position to the new connection
            game.sendPosition(jwtToken, false)
            log(gameId, "sending position info - [${game.getPositionAsJson()}]")
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach
                val move = try {
                    json.decodeFromString<Movement>(frame.readText())
                } catch (e: Exception) {
                    log(
                        gameId,
                        "error decoding client movement: frame - [$frame] stack trace - [${e.stackTraceToString()}]"
                    )
                    someThingsWentWrong("error decoding client movement")
                    return@webSocket
                }
                if (!game.isValidMove(move, jwtToken)) {
                    log(
                        gameId,
                        "received an illegal move - [$move]"
                    )
                    someThingsWentWrong("received an illegal move")
                    return@webSocket
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
                        (10 + (if (firstUserLost) secondUserRating - firstUserRating else firstUserRating - secondUserRating) / 100).coerceIn(
                            -50L..50L
                        )
                    Users.updateRatingById(game.firstUser.id().getOrThrow(), if (firstUserLost) -delta else delta)
                    Users.updateRatingById(game.secondUser.id().getOrThrow(), if (firstUserLost) delta else -delta)
                    game.sendMove(jwtToken, Movement(null, null), true)
                    game.sendMove(jwtToken, Movement(null, null), false)
                    return@webSocket
                }
            }
        } catch (e: IOException) {
            log(gameId, "uncaught exception ${e.stackTraceToString()}")
        }
    }
}

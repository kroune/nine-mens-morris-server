package com.example.routing

import com.example.*
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
import java.io.IOException

fun Route.gameRouting() {
    get("/is-playing") {
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
        val jwtToken = call.parameters["jwtToken"]
        if (jwtToken == null) {
            noJwtToken()
            return@webSocket
        }
        if (!validateJwtToken(jwtToken)) {
            jwtTokenIsNotValid()
            return@webSocket
        }
        val thisConnection = Connection(jwtToken, this)
        SearchingForGame.addUser(thisConnection)
    }
    webSocket("/game") {
        run {
            val jwtTokenParameter = call.parameters["jwtToken"]
            if (jwtTokenParameter == null) {
                noJwtToken()
                return@webSocket
            }
            if (!validateJwtToken(jwtTokenParameter)) {
                jwtTokenIsNotValid()
                return@webSocket
            }
            val gameId = call.parameters["gameId"]
            if (gameId == null) {
                noGameId()
                return@webSocket
            }
            if (gameId.toLongOrNull() == null) {
                gameIdIsNotLong()
                return@webSocket
            }
            val game = Games.getGame(gameId.toLong())
            if (game == null) {
                gameIdIsNotValid()
                return@webSocket
            }
            if (!game.isParticipating(jwtTokenParameter)) {
                jwtTokenIsNotValidForThisGame()
                return@webSocket
            }
        }
        val gameId = call.parameters["gameId"]!!.toLong()
        val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
        val game = Games.getGame(gameId)!!
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

package com.example.routing.game.ws

import com.example.*
import com.example.game.BotGenerator
import com.example.game.Connection
import com.example.game.Games
import com.example.game.SearchingForGame
import com.example.responses.ws.jwtTokenIsNotValidForThisGame
import com.example.responses.ws.someThingsWentWrong
import com.example.users.Users
import com.kr8ne.mensMorris.PIECES_TO_FLY
import com.kr8ne.mensMorris.move.Movement
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

fun Route.gameRoutingWS() {
    webSocket("/search-for-game") {
        requireValidJwtToken {
            return@webSocket
        }

        val jwtToken = call.parameters["jwtToken"]!!
        val thisConnection = Connection(jwtToken, this)
        val channel = Channel<Pair<Boolean, Long>>()
        SearchingForGame.addUser(thisConnection, channel)
        CoroutineScope(Dispatchers.IO).launch {
            this@webSocket.closeReason.join()
            log("user disconnected from searching for game", LogPriority.Debug)
            SearchingForGame.removeUser(thisConnection)
        }
        channel.consumeEach { (isWaitingTime, it) ->
            val jsonText = Json.encodeToString<Pair<Boolean, Long>>(Pair(isWaitingTime, it))
            send(jsonText)
            if (!isWaitingTime) {
                log("sending game id to the user gameId - $it", LogPriority.Debug)
                channel.close()
                close(CloseReason(CloseReason.Codes.NORMAL, it.toString()))
            }
        }
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
            val gameEndAction: suspend () -> Unit = {
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
                game.firstUser.session?.close()
                game.secondUser.session?.close()
                if (BotGenerator.isBot(game.firstUser.jwtToken.getLogin().getOrThrow())) {
                    BotGenerator.botGotFree(game.firstUser.jwtToken.getLogin().getOrThrow())
                }
                if (BotGenerator.isBot(game.secondUser.jwtToken.getLogin().getOrThrow())) {
                    BotGenerator.botGotFree(game.secondUser.jwtToken.getLogin().getOrThrow())
                }
            }
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
                // user gave up
                if (move.startIndex == null && move.endIndex == null) {
                    log(gameId, "user gave up")
                    gameEndAction()
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
                    gameEndAction()
                    return@webSocket
                }
            }
        } catch (e: IOException) {
            log(gameId, "uncaught exception ${e.stackTraceToString()}")
        }
    }
}

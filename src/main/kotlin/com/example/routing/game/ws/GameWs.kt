package com.example.routing.game.ws

import com.example.*
import com.example.game.Connection
import com.example.game.GamesDB
import com.example.game.SearchingForGame
import com.example.responses.ws.jwtTokenIsNotValidForThisGame
import com.example.responses.ws.someThingsWentWrong
import com.example.users.Users
import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
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
        val game = GamesDB.getGame(gameId)!!
        if (!game.isParticipating(jwtToken)) {
            jwtTokenIsNotValidForThisGame()
            return@webSocket
        }
        try {
            val isFirstUser: Boolean
            game.updateSession(jwtToken, this)
            val (isGreen, enemyId) = when (jwtToken) {
                game.firstUser.jwtToken -> {
                    isFirstUser = true
                    Pair(game.isFirstPlayerGreen, Users.getIdByJwtToken(game.secondUser.jwtToken).getOrThrow())
                }

                game.secondUser.jwtToken -> {
                    isFirstUser = false
                    Pair(!game.isFirstPlayerGreen, Users.getIdByJwtToken(game.firstUser.jwtToken).getOrThrow())
                }

                else -> {
                    error("this jwt token doesn't belong to this game")
                }
            }
            send(isGreen.toString())
            log(gameId, "sending isGreen info - [$isGreen]")
            // we send position to the new connection
            game.sendPosition(jwtToken, false)
            log(gameId, "sending position info")
            send(enemyId.toString())
            log(gameId, "sending enemy id info - [$enemyId]")
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach
                val move = try {
                    json.decodeFromString<Movement>(frame.readText())
                } catch (e: Exception) {
                    log(
                        gameId,
                        "error decoding client movement: frame - [${frame.frameType}] stack trace - [${e.stackTraceToString()}]"
                    )
                    someThingsWentWrong("error decoding client movement")
                    return@webSocket
                }
                // user gave up
                if (move.startIndex == null && move.endIndex == null) {
                    log(gameId, "user gave up")
                    game.handleGameEnd(GameEndReason.UserGaveUp(isFirstUser))
                    return@webSocket
                }
                if (!game.isMovePossible(move, jwtToken)) {
                    log(
                        gameId,
                        "received an illegal move - [$move]"
                    )
                    someThingsWentWrong("received an illegal move")
                    return@webSocket
                }
                // send new position to the enemy
                game.sendMove(jwtToken, move, true)
                game.applyMove(move)
                // note: checking if the game has ended happens in [GameData.applyMove]
            }
        } catch (e: IOException) {
            log(gameId, "uncaught exception ${e.stackTraceToString()}")
        }
    }
}

/*
 * This file is part of nine-mens-morris-server (https://github.com/kroune/nine-mens-morris-server)
 * Copyright (C) 2024-2024  kroune
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact: kr0ne@tuta.io
 */
package com.example.routing.game.ws

import com.example.features.LogPriority
import com.example.data.local.gamesRepository
import com.example.data.local.usersRepository
import com.example.features.game.GameDataFactory
import com.example.features.game.SearchingForGame
import com.example.common.json
import com.example.features.log
import com.example.routing.responses.requireGameId
import com.example.routing.responses.requireValidJwtToken
import com.example.routing.responses.ws.jwtTokenIsNotValidForThisGame
import com.example.routing.responses.ws.someThingsWentWrong
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
        val userId = usersRepository.getIdByJwtToken(jwtToken)!!
        val channel = Channel<Pair<Boolean, Long>>()
        SearchingForGame.addUser(userId, channel)
        CoroutineScope(Dispatchers.IO).launch {
            this@webSocket.closeReason.join()
            log("user disconnected from searching for game", LogPriority.Debug)
            SearchingForGame.removeUser(userId)
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
        val jwtToken = call.parameters["jwtToken"]!!
        val userId = usersRepository.getIdByJwtToken(jwtToken)!!
        if (!gamesRepository.participates(userId)) {
            jwtTokenIsNotValidForThisGame()
            return@webSocket
        }
        val game = GameDataFactory.getGame(gameId, userId, this)
        try {
            val isFirstUser = game.isFirstPlayer(userId)
            game.updateSession(userId, this)
            val isGreen = if (game.isFirstPlayer(userId)) {
                game.isFirstPlayerMovesFirst()
            } else {
                !game.isFirstPlayerMovesFirst()
            }
            val enemyId = game.enemyId(userId)
            send(isGreen.toString())
            log("sending isGreen info - [$isGreen]", LogPriority.Debug)
            // we send position to the new connection
            game.sendPosition(userId, opposite = false)
            log("sending position info", LogPriority.Debug)
            send(enemyId.toString())
            log("sending enemy id info - [$enemyId]", LogPriority.Debug)
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach
                val move = try {
                    json.decodeFromString<Movement>(frame.readText())
                } catch (e: Exception) {
                    log(
                        "error decoding client movement: frame - [${frame.frameType}] stack trace - [${e.stackTraceToString()}]",
                        LogPriority.Debug
                    )
                    someThingsWentWrong("error decoding client movement")
                    return@webSocket
                }
                // user gave up
                if (move.startIndex == null && move.endIndex == null) {
                    log(
                        "user gave up",
                        LogPriority.Debug
                    )
                    game.handleGameEnd(GameEndReason.UserGaveUp(isFirstUser))
                    return@webSocket
                }
                if (!game.isMovePossible(move, userId)) {
                    log(
                        "received an illegal move - [$move]",
                        LogPriority.Debug
                    )
                    someThingsWentWrong("received an illegal move")
                    return@webSocket
                }
                // send new position to the enemy
                game.sendMove(userId, move, true)
                game.applyMove(move, isFirstUser)
                // note: checking if the game has ended happens in [GameData.applyMove]
            }
        } catch (e: IOException) {
            log(
                "uncaught exception ${e.stackTraceToString()}",
                LogPriority.Debug
            )
        }
    }
}

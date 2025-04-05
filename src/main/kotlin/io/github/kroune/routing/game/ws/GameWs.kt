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
package io.github.kroune.routing.game.ws

import io.github.kroune.common.json
import io.github.kroune.common.sendSerializedEvent
import io.github.kroune.data.local.gamesRepository
import io.github.kroune.data.local.usersRepository
import io.github.kroune.features.game.GameDataFactory
import io.github.kroune.features.game.SearchingForGame
import io.github.kroune.features.game.SearchingForGameConnection
import io.github.kroune.features.logging.gameId
import io.github.kroune.features.logging.logger
import io.github.kroune.features.logging.userId
import io.github.kroune.routing.responses.requireGameId
import io.github.kroune.routing.responses.requireValidJwtToken
import io.github.kroune.routing.responses.ws.jwtTokenIsNotValidForThisGame
import io.github.kroune.routing.responses.ws.someThingsWentWrong
import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

fun Route.gameRoutingWS() {
    webSocket("/search-for-game") {
        requireValidJwtToken {
            return@webSocket
        }
        val jwtToken = call.parameters["jwtToken"]!!
        val userId = usersRepository.getIdByJwtToken(jwtToken)!!
        val expectedWaitingTime = MutableStateFlow<Long?>(null)
        val waitingTimeJob = launch {
            runCatching {
                expectedWaitingTime.collect {
                    if (it != null)
                        sendSerializedEvent(it, "waiting_time")
                }
            }.onFailure {
                logger.atInfo {
                    message = "error while sending a move"
                    cause = it
                }
            }
        }
        val onGameFound: suspend (Long) -> Unit = { data: Long ->
            sendSerializedEvent(data, "game_id")
            flush()
            waitingTimeJob.cancel()
            close()
        }
        SearchingForGame.addUser(
            userId,
            SearchingForGameConnection(expectedWaitingTime, onGameFound)
        )
        closeReason.await()
        SearchingForGame.removeUser(userId)
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
        val game = GameDataFactory.getGame(gameId)
        try {
            val isFirstUser = game.isFirstPlayer(userId)
            val isGreen = if (game.isFirstPlayer(userId)) {
                game.isFirstPlayerMovesFirst()
            } else {
                !game.isFirstPlayerMovesFirst()
            }
            val enemyId = game.enemyId(userId)
            sendSerialized(isGreen)
            logger.atDebug {
                message = "sending isGreen info - [$isGreen]"
                payload = buildMap {
                    userId(userId)
                    gameId(gameId)
                }
            }
            sendSerialized(enemyId)
            logger.atDebug {
                message = "sending enemy id info - [$enemyId]"
                payload = buildMap {
                    userId(userId)
                    gameId(gameId)
                }
            }
            // we send position to the new connections
            game.sendPosition(this)
            logger.atDebug {
                message = "sending position info"
                payload = buildMap {
                    userId(userId)
                    gameId(gameId)
                }
            }
            game.updateSession(userId, this)
            while (true) {
                val frame = this.incoming.receive()
                if (frame !is Frame.Text) continue
                val move = try {
                    json.decodeFromString<Movement>(frame.readText())
                } catch (e: Exception) {
                    logger.atDebug {
                        message = "error decoding client movement: frameType - [${frame.frameType}]"
                        payload = buildMap {
                            userId(userId)
                            gameId(gameId)
                        }
                        cause = e
                    }
                    someThingsWentWrong("error decoding client movement")
                    return@webSocket
                }
                // user gave up
                if (move.startIndex == null && move.endIndex == null) {
                    logger.atDebug {
                        message = "user gave up"
                        payload = buildMap {
                            userId(userId)
                            gameId(gameId)
                        }
                    }
                    game.handleGameEnd(GameEndReason.UserGaveUp(isFirstUser))
                    return@webSocket
                }
                if (!game.isMovePossible(move, userId)) {
                    logger.atDebug {
                        message = "received an illegal move - [$move]"
                        payload = buildMap {
                            userId(userId)
                            gameId(gameId)
                        }
                    }
                    someThingsWentWrong("received an illegal move")
                    return@webSocket
                }
                // send new position to the enemy
                game.sendMove(userId, move, true)
                game.applyMove(move, isFirstUser)
                // note: checking if the game has ended happens in [GameData.applyMove]
            }
        } catch (e: ClosedReceiveChannelException) {
            // this exception is thrown if websocket session was closed, and we tried to receive smth
            logger.atDebug {
                message = "channel was closed"
                payload = buildMap {
                    userId(userId)
                    gameId(gameId)
                }
                cause = e
            }
            return@webSocket
        } catch (e: Exception) {
            logger.atError {
                message = "uncaught exception"
                payload = buildMap {
                    userId(userId)
                    gameId(gameId)
                }
                cause = e
            }
        }
    }
}

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

import com.example.common.json
import com.example.data.local.gamesRepository
import com.example.data.local.usersRepository
import com.example.features.game.GameDataFactory
import com.example.features.game.SearchingForGame
import com.example.features.logging.gameId
import com.example.features.logging.logger
import com.example.features.logging.userId
import com.example.routing.responses.requireGameId
import com.example.routing.responses.requireValidJwtToken
import com.example.routing.responses.ws.jwtTokenIsNotValidForThisGame
import com.example.routing.responses.ws.someThingsWentWrong
import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.gameRoutingWS() {
    webSocket("/search-for-game") {
        requireValidJwtToken {
            return@webSocket
        }

        val jwtToken = call.parameters["jwtToken"]!!
        val userId = usersRepository.getIdByJwtToken(jwtToken)!!
        val channel = Channel<Pair<Boolean, Long>>(capacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        SearchingForGame.addUser(userId, channel)
        try {
            while (true) {
                val (isWaitingTime, gameId) = channel.receive()
                val jsonText = Json.encodeToString<Pair<Boolean, Long>>(Pair(isWaitingTime, gameId))
                send(jsonText)
                if (!isWaitingTime) {
                    logger.atDebug {
                        message = "sending game id to the user"
                        payload = buildMap {
                            userId(userId)
                            gameId(gameId)
                        }
                    }
                    channel.close()
                    close(CloseReason(CloseReason.Codes.NORMAL, gameId.toString()))
                    break
                }
            }
        } catch (e: ClosedSendChannelException) {
            logger.atDebug {
                message = "sending game id to the user"
                payload = buildMap {
                    userId(userId)
                }
                cause = e
            }
            SearchingForGame.removeUser(userId)
        } catch (e: ClosedReceiveChannelException) {
            logger.atDebug {
                message = "user disconnected from searching for game"
                payload = buildMap {
                    userId(userId)
                }
                cause = e
            }
            SearchingForGame.removeUser(userId)
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

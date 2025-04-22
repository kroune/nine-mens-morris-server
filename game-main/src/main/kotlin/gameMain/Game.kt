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

package gameMain

import common.ConfigurationLoader.currentConfig
import common.logging.gameId
import common.logging.globalLogger
import common.logging.userId
import com.kroune.nineMensMorrisLib.GameState
import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import gameMain.data.dao.GamesDataServiceI
import user.data.dao.UsersDataServiceI
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

object GameDataFactory {
    private val gamesCache = Collections.synchronizedMap(hashMapOf<Long, Game>())

    fun getGame(gameId: Long): Game {
        synchronized(gamesCache) {
            val cache = gamesCache[gameId]
            if (cache != null) {
                return cache
            }
            val gameClass = Game(gameId)
            gamesCache[gameId] = gameClass
            return gameClass
        }
    }
}

/**
 * if a bot exists in game - it is [secondPlayer]
 */
class Game(
    val gameId: Long,
    private var firstPlayer: DefaultWebSocketServerSession? = null,
    private var secondPlayer: DefaultWebSocketServerSession? = null,
): KoinComponent {
    private val gamesRepository by inject<GamesDataServiceI>()
    private val usersRepository by inject<UsersDataServiceI>()

    init {
        // it is possible that bot should make first move
        runBlocking {
            botMove()
        }
    }

    /**
     * takes actions when game has ended
     * @param reason - reason why the game has ended
     */
    fun handleGameEnd(
        reason: GameEndReason
    ) {
        val isFirstUserLost = reason.isFirstUser!!
        CoroutineScope(Dispatchers.Default).launch {
            globalLogger.atInfo {
                message = "Game ended due to ${reason.javaClass.simpleName}, isFirstUserLost = ${reason.isFirstUser}"
                payload = buildMap {
                    gameId(gameId)
                }
            }
            val firstPlayerId = gamesRepository.getFirstUserIdByGameId(gameId)!!
            val secondPlayerId = gamesRepository.getSecondUserIdByGameId(gameId)!!
            val firstUserRating = usersRepository.getRatingById(firstPlayerId)!!
            val secondUserRating = usersRepository.getRatingById(secondPlayerId)!!
            val delta =
                (10 + (if (isFirstUserLost) secondUserRating - firstUserRating else firstUserRating - secondUserRating) / 100).coerceIn(
                    -50..50
                )
            listOf(firstPlayerId, secondPlayerId).forEach { userId ->
                sendMove(userId, Movement(null, null), false)
                sendDataTo(userId, false, reason.javaClass.simpleName)
                if (BotProvider.isBot(userId)) {
                    BotProvider.addBotToTheFreeBotsQueue(userId)
                }
            }
            globalLogger.atInfo {
                message = "starting to delete game"
                payload = buildMap {
                    gameId(gameId)
                }
            }
            gamesRepository.delete(gameId)
            usersRepository.updateRatingById(firstPlayerId, if (isFirstUserLost) -delta else delta)
            usersRepository.updateRatingById(secondPlayerId, if (isFirstUserLost) delta else -delta)
            CoroutineScope(Dispatchers.Default).launch {
                withTimeout(20.seconds) {
                    globalLogger.atInfo {
                        message = "sessions closed for firstPlayer"
                        payload = buildMap {
                            gameId(gameId)
                            userId(firstPlayerId)
                        }
                    }
                    firstPlayer?.close()
                }
            }
            CoroutineScope(Dispatchers.Default).launch {
                withTimeout(20.seconds) {
                    globalLogger.atInfo {
                        message = "sessions closed for secondPlayer"
                        payload = buildMap {
                            gameId(gameId)
                            userId(secondPlayerId)
                        }
                    }
                    secondPlayer?.close()
                }
            }
        }
    }


    suspend fun sendMove(userId: Long, movement: Movement, opposite: Boolean) {
        val move = Json.encodeToString<Movement>(movement)
        // TODO: make it wait for end of sending position
        sendDataTo(
            userId = userId, opposite = opposite, data = move
        )
    }

    /**
     * send data [String] to the needed player
     *
     * @param userId id of the player
     * @param opposite if data should be sent to the opposite of the current player
     *
     * @throws IllegalStateException if jwt token doesn't much either of player
     */
    private suspend fun sendDataTo(userId: Long, opposite: Boolean, data: String) {
        val firstUserId = gamesRepository.getFirstUserIdByGameId(gameId)!!
        val secondUserId = gamesRepository.getSecondUserIdByGameId(gameId)!!
        try {
            withTimeout(15.seconds) {
                when (userId) {
                    firstUserId -> {
                        val sendToFirstUser = !opposite
                        if (sendToFirstUser) {
                            firstPlayer?.send(data)
                            globalLogger.atDebug {
                                message = "sent \"$data\""
                                payload = buildMap {
                                    userId(firstUserId)
                                }
                            }
                        } else {
                            secondPlayer?.send(data)
                            globalLogger.atDebug {
                                message = "sent \"$data\""
                                payload = buildMap {
                                    userId(secondUserId)
                                }
                            }
                        }
                    }

                    secondUserId -> {
                        val sendToSecondUser = !opposite
                        if (sendToSecondUser) {
                            secondPlayer?.send(data)
                            globalLogger.atDebug {
                                message = "sent \"$data\""
                                payload = buildMap {
                                    userId(secondUserId)
                                }
                            }
                        } else {
                            firstPlayer?.send(data)
                            globalLogger.atDebug {
                                message = "sent \"$data\""
                                payload = buildMap {
                                    userId(firstUserId)
                                }
                            }
                        }
                    }

                    else -> {
                        error("jwt token must either belong to the first user or to second one")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            globalLogger.atWarn {
                message = "reached timeout for sending data"
                cause = e
            }
        } catch (_: ClosedSendChannelException) {
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            globalLogger.atError {
                message = "uncaught exception when sending data"
                cause = e
            }
        }
    }

    /**
     * sends position to the user
     * happens when the user firstly connected
     *
     * @throws SerializationException if encoding failed
     * @throws IllegalArgumentException if encoding failed
     */
    suspend fun sendPosition(session: DefaultWebSocketServerSession) {
        val position = gamesRepository.getPositionByGameId(gameId)!!
        session.sendSerialized(position)
    }

    /**
     * tells if provided move is possible
     *
     * @param move move to check
     * @param userId id of the player
     */
    suspend fun isMovePossible(move: Movement, userId: Long): Boolean {
        val firstUserId = gamesRepository.getFirstUserIdByGameId(gameId)!!
        val secondUserId = gamesRepository.getSecondUserIdByGameId(gameId)!!
        val position = gamesRepository.getPositionByGameId(gameId)!!
        val firstPlayerMovesFirst = gamesRepository.getFirstPlayerMovesFirstByGameId(gameId)!!
        if (firstUserId == userId) {
            return position.generateMoves().contains(move) && position.pieceToMove == firstPlayerMovesFirst
        }
        if (secondUserId == userId) {
            return position.generateMoves().contains(move) && position.pieceToMove == !firstPlayerMovesFirst
        }
        return false
    }

    private suspend fun botMove() {
        val firstUserId = gamesRepository.getFirstUserIdByGameId(gameId)!!
        val secondUserId = gamesRepository.getSecondUserIdByGameId(gameId)!!
        val botUserId = gamesRepository.getBotIdByGameId(gameId)
        val position = gamesRepository.getPositionByGameId(gameId)!!
        val firstPlayerMovesFirst = gamesRepository.getFirstPlayerMovesFirstByGameId(gameId)!!
        // if bot should make move
        val isFirstPlayerBot = firstUserId == botUserId
        val isSecondPlayerBot = secondUserId == botUserId
        val firstPlayerMoves = position.pieceToMove == firstPlayerMovesFirst
        val botExistsAndCanMakeMove = (firstPlayerMoves && isFirstPlayerBot) || (!firstPlayerMoves && isSecondPlayerBot)
        if (botExistsAndCanMakeMove) {
            CoroutineScope(Dispatchers.Default).launch {
                val newMove = position.findBestMove(Random.nextInt(2, 4).toUByte()) ?: error("no move found")
                // this shouldn't cause stackoverflow, since you can move at max 3 times in a row
                applyMove(newMove, isFirstPlayerBot)
                // in one of those cases move won't be sent (since one user is bot)
                sendMove(firstUserId, newMove, false)
                sendMove(secondUserId, newMove, false)
            }
        }
    }

    private val timeForMove = currentConfig.gameConfig.timeForMove

    suspend fun applyMove(move: Movement, isFirstPlayerPerformedMove: Boolean) {
        gamesRepository.applyMove(gameId, move)
        val previousMoveCount = gamesRepository.getMovesCountByGameId(gameId)
        val position = gamesRepository.getPositionByGameId(gameId)!!
        if (position.gameState() == GameState.End || position.generateMoves().isEmpty()) {/*
             * if game has ended after players move it means, that other player lost
             * because game ends when user can't make a move | has less than 3 pieces
             *
             * move performer got removal move after another move
             * enemy couldn't perform any move yet, so move performer can always "undo" his move,
             * so he always has a move to perform
             *
             * pieces count couldn't decrease either
             *
             * that means that the other player lost (not the one, who performed the move)
             */
            val firstPlayerWon = !isFirstPlayerPerformedMove
            handleGameEnd(GameEndReason.Normal(firstPlayerWon))
        } else {
            botMove()
            CoroutineScope(Dispatchers.IO).launch {
                delay(timeForMove)
                val currentMoveCount = gamesRepository.getMovesCountByGameId(gameId)
                // if no moves were performed
                if (currentMoveCount == previousMoveCount) {
                    val firstPlayerMovesFirst = gamesRepository.getFirstPlayerMovesFirstByGameId(gameId)
                    val firstUserWon = position.pieceToMove != firstPlayerMovesFirst
                    handleGameEnd(GameEndReason.UserWasTooSlow(firstUserWon))
                }
            }
        }
    }

    suspend fun isFirstPlayerMovesFirst(): Boolean {
        return gamesRepository.getFirstPlayerMovesFirstByGameId(gameId)!!
    }

    suspend fun isFirstPlayer(userId: Long): Boolean {
        val firstPlayerId = gamesRepository.getFirstUserIdByGameId(gameId)
        val secondPlayerId = gamesRepository.getSecondUserIdByGameId(gameId)
        return when (userId) {
            firstPlayerId -> {
                true
            }

            secondPlayerId -> {
                false
            }

            else -> {
                error("not a participant")
            }
        }
    }

    /**
     * updates user session in order to send data successfully
     *
     * @param userId id of the player
     * @param session new session of this player
     */
    suspend fun updateSession(userId: Long, session: DefaultWebSocketServerSession) {
        val firstPlayerId = gamesRepository.getFirstUserIdByGameId(gameId)!!
        val secondPlayerId = gamesRepository.getSecondUserIdByGameId(gameId)!!
        when (userId) {
            firstPlayerId -> {
                firstPlayer = session
            }

            secondPlayerId -> {
                secondPlayer = session
            }

            else -> {
                error("")
            }
        }
    }

    suspend fun enemyId(userId: Long): Long {
        val firstPlayerId = gamesRepository.getFirstUserIdByGameId(gameId)!!
        val secondPlayerId = gamesRepository.getSecondUserIdByGameId(gameId)!!
        return if (isFirstPlayer(userId)) secondPlayerId else firstPlayerId
    }
}

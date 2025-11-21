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

import botsApi.BotProviderI
import com.kroune.nineMensMorrisLib.GameState
import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import common.BlockingFetchRequest
import common.ConfigurationLoader.currentConfig
import common.closeWithTimeout
import common.fireAndForgetScope
import common.logging.gameId
import common.logging.globalLogger
import common.logging.userId
import gameCommon.GameI
import gameCommon.data.dao.GamesDataServiceI
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import userApi.data.dao.UsersDataServiceI
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * if a bot exists in game - it is [secondPlayer]
 */
internal class Game(
    override val gameId: Long,
    override var firstPlayer: DefaultWebSocketServerSession?,
    override var secondPlayer: DefaultWebSocketServerSession?,
    private val gamesRepository: GamesDataServiceI,
    private val usersRepository: UsersDataServiceI,
    private val botProvider: BotProviderI,
) : KoinComponent, GameI() {
    private val gameScope = CoroutineScope(Dispatchers.IO)

    private val firstUserId: Long by BlockingFetchRequest(gameScope) {
        gamesRepository.getFirstUserIdByGameId(gameId)!!
    }
    private val secondUserId: Long by BlockingFetchRequest(gameScope) {
        gamesRepository.getSecondUserIdByGameId(gameId)!!
    }
    private val botUserId: Long? by BlockingFetchRequest(gameScope) {
        gamesRepository.getBotIdByGameId(gameId)
    }


    override suspend fun initializeGame() {
        timeoutCheck(0)
        botMove()
    }

    override fun timeoutCheck(previousMoveCount: Int?) {
        gameScope.launch {
            delay(timeForMove)
            val currentMoveCount = gamesRepository.getMovesCountByGameId(gameId)
            // if no moves were performed
            if (currentMoveCount == previousMoveCount) {
                val firstPlayerMovesFirst = gamesRepository.getFirstPlayerMovesFirstByGameId(gameId)
                val position = gamesRepository.getPositionByGameId(gameId)!!
                val firstUserWon = position.pieceToMove != firstPlayerMovesFirst
                handleGameEnd(GameEndReason.UserWasTooSlow(firstUserWon))
            }
        }
    }

    /**
     * takes actions when game has ended
     * @param reason - reason why the game has ended
     */
    override fun handleGameEnd(
        reason: GameEndReason,
    ) {
        val isFirstUserLost = reason.isFirstUser!!
        globalLogger.atInfo {
            message = "Game ended due to ${reason.javaClass.simpleName}, isFirstUserLost = ${reason.isFirstUser}"
            payload = buildMap {
                gameId(gameId)
            }
        }

        listOf(firstUserId to firstPlayer, secondUserId to secondPlayer).map { (playerId, playerSession) ->
            fireAndForgetScope.launch {
                sendMove(playerId, Movement(null, null), false)
                sendDataTo(playerId, false, reason.javaClass.simpleName)
                if (botProvider.isBot(playerId)) {
                    botProvider.addBotToTheFreeBotsQueue(playerId)
                }
                globalLogger.atInfo {
                    message = "sessions closing"
                    payload = buildMap {
                        gameId(gameId)
                        userId(playerId)
                    }
                }
                playerSession?.closeWithTimeout()
            }
        }
        fireAndForgetScope.launch {
            globalLogger.atInfo {
                message = "starting to delete game"
                payload = buildMap {
                    gameId(gameId)
                }
            }
            val firstUserRating = usersRepository.getRatingById(firstUserId)!!
            val secondUserRating = usersRepository.getRatingById(secondUserId)!!
            val delta =
                (10 + (if (isFirstUserLost) secondUserRating - firstUserRating else firstUserRating - secondUserRating) / 100)
                    .coerceIn(-50..50)
            gamesRepository.delete(gameId)
            usersRepository.updateRatingById(firstUserId, if (isFirstUserLost) -delta else delta)
            usersRepository.updateRatingById(secondUserId, if (isFirstUserLost) delta else -delta)
        }
        gameScope.cancel()
    }


    override suspend fun sendMove(userId: Long, movement: Movement, opposite: Boolean) {
        val move = Json.encodeToString<Movement>(movement)
        // TODO: make it wait for end of sending position
        sendDataTo(
            userId = userId, opposite = opposite, data = move,
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
    private suspend fun sendDataTo(
        userId: Long,
        opposite: Boolean,
        data: String
    ) {
        try {
            val (playerSession, playerId) = when (userId) {
                firstUserId -> {
                    val sendToFirstUser = !opposite
                    if (sendToFirstUser) {
                        firstPlayer to firstUserId
                    } else {
                        secondPlayer to secondUserId
                    }
                }

                secondUserId -> {
                    val sendToSecondUser = !opposite
                    if (sendToSecondUser) {
                        secondPlayer to secondUserId
                    } else {
                        firstPlayer to firstUserId
                    }
                }

                else -> {
                    error("jwt token must either belong to the first user or to second one")
                }
            }
            withTimeout(15.seconds) {
                playerSession?.send(data)
            }
            globalLogger.atDebug {
                message = "sent \"$data\""
                payload = buildMap {
                    userId(playerId)
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
    override suspend fun sendPosition(session: DefaultWebSocketServerSession) {
        val position = gamesRepository.getPositionByGameId(gameId)!!
        session.sendSerialized(position)
    }

    /**
     * tells if provided move is possible
     *
     * @param move move to check
     * @param userId id of the player
     */
    override suspend fun isMovePossible(move: Movement, userId: Long): Boolean {
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
        val position = gamesRepository.getPositionByGameId(gameId)!!
        val firstPlayerMovesFirst = gamesRepository.getFirstPlayerMovesFirstByGameId(gameId)!!
        // if bot should make move
        val isFirstPlayerBot = firstUserId == botUserId
        val isSecondPlayerBot = secondUserId == botUserId
        val firstPlayerMoves = position.pieceToMove == firstPlayerMovesFirst
        val botExistsAndCanMakeMove = (firstPlayerMoves && isFirstPlayerBot) || (!firstPlayerMoves && isSecondPlayerBot)
        if (botExistsAndCanMakeMove) {
            val newMove = position.findBestMove(Random.nextInt(2, 4).toUByte()) ?: error("no move found")
            // in one of those cases move won't be sent (since one user is bot)
            when (isFirstPlayerBot) {
                true -> {
                    sendMove(secondUserId, newMove, false)
                }

                false -> {
                    sendMove(firstUserId, newMove, false)
                }
            }
            applyMove(newMove, isFirstPlayerBot)
        }
    }

    private val timeForMove = currentConfig.gameConfig.timeForMove

    override suspend fun applyMove(move: Movement, isFirstPlayerPerformedMove: Boolean) {
        gamesRepository.applyMove(gameId, move)
        val previousMoveCount = gamesRepository.getMovesCountByGameId(gameId)
        val position = gamesRepository.getPositionByGameId(gameId)!!
        if (position.gameState() == GameState.End) {
            /*
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
            timeoutCheck(previousMoveCount)
        }
    }

    override suspend fun isFirstPlayerMovesFirst(): Boolean {
        return gamesRepository.getFirstPlayerMovesFirstByGameId(gameId)!!
    }

    override fun isFirstPlayer(userId: Long): Boolean {
        return when (userId) {
            firstUserId -> {
                true
            }

            secondUserId -> {
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
    override fun updateSession(userId: Long, session: DefaultWebSocketServerSession) {
        when (userId) {
            firstUserId -> {
                firstPlayer = session
            }

            secondUserId -> {
                secondPlayer = session
            }

            else -> {
                error("")
            }
        }
    }

    override fun enemyId(userId: Long): Long {
        return if (isFirstPlayer(userId)) secondUserId else firstUserId
    }
}

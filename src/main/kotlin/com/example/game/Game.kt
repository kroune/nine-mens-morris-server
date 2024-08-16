package com.example.game

import com.example.currentConfig
import com.example.data.gamesRepository
import com.example.data.usersRepository
import com.example.json
import com.example.log
import com.kroune.nineMensMorrisLib.GameState
import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import io.ktor.server.application.ApplicationCall
import io.ktor.server.websocket.*
import io.ktor.util.InternalAPI
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import java.util.Collections
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

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
 * if a bot exists in game - it is [secondUser]
 */
class Game(
    private val gameId: Long,
    private var firstPlayer: DefaultWebSocketServerSession = EmptySession,
    private var secondPlayer: DefaultWebSocketServerSession = EmptySession,
) {
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
        val isFirstUserLost = reason.isFirstUser
        CoroutineScope(Dispatchers.Default).launch {
            val firstPlayerId = gamesRepository.getFirstUserIdByGameId(gameId)!!
            val secondPlayerId = gamesRepository.getSecondUserIdByGameId(gameId)!!
            val firstUserRating = usersRepository.getRatingById(firstPlayerId)!!
            val secondUserRating = usersRepository.getRatingById(secondPlayerId)!!
            val delta =
                (10 + (if (isFirstUserLost) secondUserRating - firstUserRating else firstUserRating - secondUserRating) / 100).coerceIn(
                    -50..50
                )
            usersRepository.updateRatingById(firstPlayerId, if (isFirstUserLost) -delta else delta)
            usersRepository.updateRatingById(secondPlayerId, if (isFirstUserLost) delta else -delta)
            listOf(firstPlayerId, secondPlayerId).forEach { userId ->
                sendMove(userId, Movement(null, null), false)
                sendDataTo(userId, false, "game ended")
                if (BotProvider.isBot(userId)) {
                    BotProvider.botGotFree(userId)
                }
            }
            firstPlayer.close()
            secondPlayer.close()
        }
    }


    suspend fun sendMove(userId: Long, movement: Movement, opposite: Boolean) {
        val move = json.encodeToString<Movement>(movement)
        sendDataTo(
            userId = userId,
            opposite = opposite,
            data = move
        )
    }

    /**
     * send data [String] to the needed player
     *
     * @param jwtToken jwtToken of the player, needed for easier calculation of the player to send
     * @param opposite if data should be sent to the opposite of the current player
     *
     * @throws IllegalStateException if jwt token doesn't much either of player
     */
    private suspend fun sendDataTo(userId: Long, opposite: Boolean, data: String) {
        val firstUserId = gamesRepository.getFirstUserIdByGameId(gameId)!!
        val secondUserId = gamesRepository.getSecondUserIdByGameId(gameId)!!
        when (userId) {
            firstUserId -> {
                val sendToFirstUser = !opposite
                if (sendToFirstUser) {
                    firstPlayer.send(data)
                } else {
                    secondPlayer.send(data)
                }
            }

            secondUserId -> {
                val sendToSecondUser = !opposite
                if (sendToSecondUser) {
                    secondPlayer.send(data)
                } else {
                    firstPlayer.send(data)
                }
            }

            else -> {
                error("jwt token must either belong to the first user or to second one")
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
    suspend fun sendPosition(userId: Long, opposite: Boolean) {
        val position = gamesRepository.getPositionByGameId(gameId)!!
        val pos = json.encodeToString<Position>(position)
        sendDataTo(
            userId = userId,
            opposite = opposite,
            data = pos
        )
    }

    /**
     * tells if provided move is possible
     *
     * @param move move to check
     * @param jwtToken jwt token of the player, who tries performed such move
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
        val botUserId = gamesRepository.getBotIdByGameId(gameId)!!
        val position = gamesRepository.getPositionByGameId(gameId)!!
        val firstPlayerMovesFirst = gamesRepository.getFirstPlayerMovesFirstByGameId(gameId)!!
        // if bot should make move
        val isFirstPlayerBot = firstUserId == botUserId
        val isSecondPlayerBot = secondUserId == botUserId
        val firstPlayerMoves = position.pieceToMove == firstPlayerMovesFirst
        val botExistsAndCanMakeMove =
            (firstPlayerMoves && isFirstPlayerBot) || (!firstPlayerMoves && isSecondPlayerBot)
        if (botExistsAndCanMakeMove) {
            println("bot making a move...")
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
        val previousMoveCount = gamesRepository.getMovesCountByGameId(gameId)
        gamesRepository.applyMove(gameId, move)
        val currentMoveCount = gamesRepository.getMovesCountByGameId(gameId)
        val position = gamesRepository.getPositionByGameId(gameId)!!
        if (position.gameState() == GameState.End || position.generateMoves().isEmpty()) {
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
            CoroutineScope(Dispatchers.IO).launch {
                delay(timeForMove)
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
        val secondPlayerId = gamesRepository.getFirstUserIdByGameId(gameId)
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
     * @param jwtToken jwt token of the player
     * @param session new session of this player
     */
    suspend fun updateSession(userId: Long, session: DefaultWebSocketServerSession) {
        val firstPlayerId = gamesRepository.getFirstUserIdByGameId(gameId)!!
        val secondPlayerId = gamesRepository.getFirstUserIdByGameId(gameId)!!
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
        val secondPlayerId = gamesRepository.getFirstUserIdByGameId(gameId)!!
        return if (isFirstPlayer(userId)) secondPlayerId else firstPlayerId
    }
}

object EmptySession : DefaultWebSocketServerSession {
    override suspend fun send(frame: Frame) {
        log("sending frame in empty session")
    }

    override suspend fun flush() {
        log("flushing in empty session")
    }

    override val closeReason: Deferred<CloseReason?> = notGonnaNeedThis
    override var pingIntervalMillis: Long = notGonnaNeedThis
    override var timeoutMillis: Long = notGonnaNeedThis

    @InternalAPI
    override fun start(negotiatedExtensions: List<WebSocketExtension<*>>) = notGonnaNeedThis
    override val extensions: List<WebSocketExtension<*>> = notGonnaNeedThis
    override val incoming: ReceiveChannel<Frame> = notGonnaNeedThis
    override var masking: Boolean = notGonnaNeedThis
    override var maxFrameSize: Long = notGonnaNeedThis
    override val outgoing: SendChannel<Frame> = notGonnaNeedThis

    @Deprecated("Use cancel() instead.", ReplaceWith("cancel()", "kotlinx.coroutines.cancel"))
    override fun terminate() = notGonnaNeedThis
    override val coroutineContext: CoroutineContext = notGonnaNeedThis
    override val call: ApplicationCall = notGonnaNeedThis
    val notGonnaNeedThis: Nothing
        get() {
            error("this shouldn't be used")
        }
}
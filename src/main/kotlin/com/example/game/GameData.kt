package com.example.game

import com.example.*
import com.example.users.Users
import com.kroune.nineMensMorrisLib.GameState
import com.kroune.nineMensMorrisLib.PIECES_TO_FLY
import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.gameStartPosition
import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class GameData(val firstUser: Connection, val secondUser: Connection) {
    private var position: Position = gameStartPosition
    val isFirstPlayerGreen = Random.nextBoolean()
    private var playedMoves = AtomicInteger(0)

    init {
        // it is possible that bot should make first move
        botMove()
    }

    /**
     * takes actions when game has ended
     * @param reason - reason why the game has ended
     */
    fun handleGameEnd(
        reason: GameEndReason = GameEndReason.Normal(run {
            val greenLost = position.greenPiecesAmount < PIECES_TO_FLY
            val firstUserLost = greenLost == isFirstPlayerGreen
            firstUserLost
        })
    ) {
        val isFirstUserLost = reason.isFirstUser
        CoroutineScope(Dispatchers.Default).launch {
            val firstUserRating = Users.getRatingById(firstUser.id().getOrThrow()).getOrThrow()
            val secondUserRating = Users.getRatingById(secondUser.id().getOrThrow()).getOrThrow()
            val delta =
                (10 + (if (isFirstUserLost) secondUserRating - firstUserRating else firstUserRating - secondUserRating) / 100).coerceIn(
                    -50L..50L
                )
            Users.updateRatingById(firstUser.id().getOrThrow(), if (isFirstUserLost) -delta else delta)
            Users.updateRatingById(secondUser.id().getOrThrow(), if (isFirstUserLost) delta else -delta)
            listOf(firstUser, secondUser).forEach { user ->
                sendMove(user.jwtToken, Movement(null, null), false)
                sendDataTo(user.jwtToken, false, "")
                user.session?.close()
                if (BotGenerator.isBot(user.getLogin())) {
                    BotGenerator.botGotFree(user.getLogin())
                }
            }
        }
    }


    suspend fun sendMove(jwtToken: CustomJwtToken, movement: Movement, opposite: Boolean) {
        val move = json.encodeToString<Movement>(movement)
        sendDataTo(
            jwtToken = jwtToken,
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
    private suspend fun sendDataTo(jwtToken: CustomJwtToken, opposite: Boolean, data: String) {
        log("sending data to user $data", LogPriority.Debug)
        when (jwtToken) {
            firstUser.jwtToken -> {
                val user = if (opposite) secondUser else firstUser
                user.session?.send(data)
            }

            secondUser.jwtToken -> {
                val user = if (opposite) firstUser else secondUser
                user.session?.send(data)
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
    suspend fun sendPosition(jwtToken: CustomJwtToken, opposite: Boolean) {
        val pos = json.encodeToString<Position>(position)
        sendDataTo(
            jwtToken = jwtToken,
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
    fun isMovePossible(move: Movement, jwtToken: CustomJwtToken): Boolean {
        if (firstUser.jwtToken == jwtToken) {
            return position.generateMoves().contains(move) && position.pieceToMove == isFirstPlayerGreen
        }
        if (secondUser.jwtToken == jwtToken) {
            return position.generateMoves().contains(move) && position.pieceToMove == !isFirstPlayerGreen
        }
        return false
    }

    private fun botMove() {
        // if bot should make move
        val firstPlayerIsBotAndCanMakeAMove = position.pieceToMove == isFirstPlayerGreen && firstUser.session == null
        val secondPlayerIsBotAndCanMakeAMove = position.pieceToMove != isFirstPlayerGreen && secondUser.session == null
        if (firstPlayerIsBotAndCanMakeAMove || secondPlayerIsBotAndCanMakeAMove) {
            CoroutineScope(Dispatchers.Default).launch {
                val newMove = position.findBestMove(Random.nextInt(2, 4).toUByte()) ?: error("no move found")
                // this shouldn't cause stackoverflow, since you can move at max 3 times in a row
                applyMove(newMove)
                // in one of those cases move won't be sent (since one user is bot)
                sendMove(firstUser.jwtToken, newMove, false)
                sendMove(secondUser.jwtToken, newMove, false)
            }
        }
    }

    private val timeForMove = currentConfig.gameConfig.timeForMove

    fun applyMove(move: Movement) {
        position = move.producePosition(position)
        val currentMoveCount = playedMoves.incrementAndGet()
        botMove()
        if (position.gameState() == GameState.End || position.generateMoves().isEmpty()) {
            handleGameEnd()
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                delay(timeForMove)
                // if no moves were performed
                if (playedMoves.get() == currentMoveCount) {
                    val firstUserLost = position.pieceToMove == isFirstPlayerGreen
                    log("game was ended, because no move were performed in $timeForMove")
                    handleGameEnd(GameEndReason.UserWasTooSlow(firstUserLost))
                }
            }
        }
    }

    fun isParticipating(jwtToken: String): Boolean {
        val jwtTokenObject = CustomJwtToken(jwtToken)
        return isParticipating(jwtTokenObject)
    }

    /**
     * @return if the user participates in the game
     */
    fun isParticipating(jwtToken: CustomJwtToken): Boolean {
        return firstUser.jwtToken == jwtToken || secondUser.jwtToken == jwtToken
    }

    fun updateSession(jwtToken: String, session: DefaultWebSocketServerSession) {
        val jwtTokenObject = CustomJwtToken(jwtToken)
        updateSession(jwtTokenObject, session)
    }

    /**
     * updates user session in order to send data successfully
     *
     * @param jwtToken jwt token of the player
     * @param session new session of this player
     */
    fun updateSession(jwtToken: CustomJwtToken, session: DefaultWebSocketServerSession) {
        when (jwtToken) {
            firstUser.jwtToken -> {
                firstUser.session = session
            }

            secondUser.jwtToken -> {
                secondUser.session = session
            }

            else -> {
                error("jwt token must either belong to the first user or to second one")
            }
        }
    }
}

/**
 * @param jwtToken user jwt token
 * @param session user session or null if player is bot
 */
class Connection(
    var jwtToken: CustomJwtToken,
    var session: DefaultWebSocketServerSession?,
) {
    constructor(jwtToken: String, session: DefaultWebSocketServerSession?) : this(CustomJwtToken(jwtToken), session)

    fun rating(): Result<Long> {
        return Users.getRatingById(id().getOrThrow())
    }

    fun getLogin(): String {
        return jwtToken.getLogin().getOrThrow()
    }

    fun id(): Result<Long> {
        return Users.getIdByJwtToken(jwtToken)
    }
}

package com.example.game

import com.example.currentConfig
import com.example.json
import com.example.data.usersRepository
import com.example.log
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

/**
 * if a bot exists in game - it is [secondUser]
 */
class GameData(val firstUser: Connection, val secondUser: Connection, private val gameId: Long, private val botAtGame: Boolean ) {
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
            val firstUserRating = usersRepository.getRatingById(firstUser.id())!!
            val secondUserRating = usersRepository.getRatingById(secondUser.id())!!
            val delta =
                (10 + (if (isFirstUserLost) secondUserRating - firstUserRating else firstUserRating - secondUserRating) / 100).coerceIn(
                    -50..50
                )
            usersRepository.updateRatingById(firstUser.id(), if (isFirstUserLost) -delta else delta)
            usersRepository.updateRatingById(secondUser.id(), if (isFirstUserLost) delta else -delta)
            listOf(firstUser, secondUser).forEach { user ->
                sendMove(user.login, Movement(null, null), false)
                sendDataTo(user.login, false, "")
                user.session?.close()
                if (BotGenerator.isBot(user.login)) {
                    BotGenerator.botGotFree(user.login)
                }
            }
        }
    }


    suspend fun sendMove(login: String, movement: Movement, opposite: Boolean) {
        val move = json.encodeToString<Movement>(movement)
        sendDataTo(
            login = login,
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
    private suspend fun sendDataTo(login: String, opposite: Boolean, data: String) {
        log(gameId, "sending data to user $data")
        when (login) {
            firstUser.login -> {
                val user = if (opposite) secondUser else firstUser
                user.session?.send(data)
            }

            secondUser.login -> {
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
    suspend fun sendPosition(login: String, opposite: Boolean) {
        val pos = json.encodeToString<Position>(position)
        sendDataTo(
            login = login,
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
    fun isMovePossible(move: Movement, login: String): Boolean {
        if (firstUser.login == login) {
            return position.generateMoves().contains(move) && position.pieceToMove == isFirstPlayerGreen
        }
        if (secondUser.login == login) {
            return position.generateMoves().contains(move) && position.pieceToMove == !isFirstPlayerGreen
        }
        return false
    }

    private fun botMove() {
        // if bot should make move
        val botExistsAndCanMakeAMove = position.pieceToMove != isFirstPlayerGreen && botAtGame
        if (botExistsAndCanMakeAMove) {
            CoroutineScope(Dispatchers.Default).launch {
                val newMove = position.findBestMove(Random.nextInt(2, 4).toUByte()) ?: error("no move found")
                // this shouldn't cause stackoverflow, since you can move at max 3 times in a row
                applyMove(newMove)
                // in one of those cases move won't be sent (since one user is bot)
                sendMove(firstUser.login, newMove, false)
                sendMove(secondUser.login, newMove, false)
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
                    log(gameId, "game was ended, because no move were performed in $timeForMove")
                    handleGameEnd(GameEndReason.UserWasTooSlow(firstUserLost))
                }
            }
        }
    }

    /**
     * @return if the user participates in the game
     */
    fun isParticipating(login: String): Boolean {
        return firstUser.login == login || secondUser.login == login
    }

    /**
     * updates user session in order to send data successfully
     *
     * @param jwtToken jwt token of the player
     * @param session new session of this player
     */
    fun updateSession(login: String, session: DefaultWebSocketServerSession) {
        when (login) {
            firstUser.login -> {
                firstUser.session = session
            }

            secondUser.login -> {
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
    var login: String,
    var session: DefaultWebSocketServerSession?,
) {

    suspend fun rating(): Int {
        return usersRepository.getRatingById(id())!!
    }

    suspend fun id(): Long {
        return usersRepository.getIdByLogin(login)!!
    }
}

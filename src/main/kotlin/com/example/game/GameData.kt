package com.example.game

import com.example.CustomJwtToken
import com.example.json
import com.example.users.Users
import com.kr8ne.mensMorris.GameState
import com.kr8ne.mensMorris.Position
import com.kr8ne.mensMorris.gameStartPosition
import com.kr8ne.mensMorris.move.Movement
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlin.random.Random

class GameData(val firstUser: Connection, val secondUser: Connection) {
    var position: Position = gameStartPosition
    val isFirstPlayerGreen = Random.nextBoolean()

    init {
        // it is possible that bot should make first move-
        botMove()
    }

    fun getPositionAsJson(): String {
        val result = json.encodeToString<Position>(position)
        return result
    }


    suspend fun sendMove(jwtToken: CustomJwtToken, movement: Movement, opposite: Boolean) {
        val move = json.encodeToString<Movement>(movement)
        when {
            firstUser.jwtToken == jwtToken -> {
                val user = if (opposite) secondUser else firstUser
                user.session?.send(move)
            }

            secondUser.jwtToken == jwtToken -> {
                val user = if (opposite) firstUser else secondUser
                user.session?.send(move)
            }

            else -> {
                error("")
            }
        }
        println(move)
    }

    suspend fun sendPosition(jwtToken: CustomJwtToken, opposite: Boolean) {
        val pos = getPositionAsJson()
        when {
            firstUser.jwtToken == jwtToken -> {
                val user = if (opposite) secondUser else firstUser
                user.session?.send(pos)
            }

            secondUser.jwtToken == jwtToken -> {
                val user = if (opposite) firstUser else secondUser
                user.session?.send(pos)
            }

            else -> {
                error("")
            }
        }
    }

    fun isValidMove(move: Movement, jwtToken: CustomJwtToken): Boolean {
        if (firstUser.jwtToken == jwtToken) {
            return position.generateMoves().contains(move) && position.pieceToMove == isFirstPlayerGreen
        }
        if (secondUser.jwtToken == jwtToken) {
            return position.generateMoves().contains(move) && position.pieceToMove == !isFirstPlayerGreen
        }
        return false
    }

    fun botMove() {
        // if bot should make move
        if (position.pieceToMove == isFirstPlayerGreen && firstUser.session == null) {
            CoroutineScope(Dispatchers.Default).launch {
                val newMove = position.findBestMove(Random.nextInt(2, 4).toUByte())
                if (newMove == null) {
                    println("no move found")
                    return@launch
                }
                println("new bot move")
                // this shouldn't cause stackoverflow, since you can move at max 3 times in a row
                applyMove(newMove)
            }
        }
        if (position.pieceToMove != isFirstPlayerGreen && secondUser.session == null) {
            CoroutineScope(Dispatchers.Default).launch {
                val newMove = position.findBestMove(Random.nextInt(2, 4).toUByte())
                if (newMove == null) {
                    println("no move found")
                    return@launch
                }
                println("new bot move")
                // this shouldn't cause stackoverflow, since you can move at max 3 times in a row
                applyMove(newMove)
            }
        }
    }
    fun applyMove(move: Movement) {
        position = move.producePosition(position)
        botMove()
    }

    fun hasEnded(): Boolean {
        return position.gameState() == GameState.End
    }

    fun isParticipating(jwtToken: String): Boolean {
        val jwtTokenObject = CustomJwtToken(jwtToken)
        return isParticipating(jwtTokenObject)
    }

    fun isParticipating(jwtToken: CustomJwtToken): Boolean {
        return firstUser.jwtToken == jwtToken || secondUser.jwtToken == jwtToken
    }

    fun updateSession(jwtToken: String, session: DefaultWebSocketServerSession) {
        val jwtTokenObject = CustomJwtToken(jwtToken)
        updateSession(jwtTokenObject, session)
    }

    fun updateSession(jwtToken: CustomJwtToken, session: DefaultWebSocketServerSession) {
        if (firstUser.jwtToken == jwtToken) {
            firstUser.session = session
        }
        if (secondUser.jwtToken == jwtToken) {
            secondUser.session = session
        }
    }
}

/**
 * @param jwtToken user jwt token
 * @param session user session or null if player is bot
 */
class Connection(
    var jwtToken: CustomJwtToken,
    var session: DefaultWebSocketSession?,
) {
    constructor(jwtToken: String, session: DefaultWebSocketSession?) : this(CustomJwtToken(jwtToken), session)

    fun id(): Result<Long> {
        return Users.getIdByJwtToken(jwtToken)
    }
}

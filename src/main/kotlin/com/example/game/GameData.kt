package com.example.game

import com.example.jwtToken.CustomJwtToken
import com.kr8ne.mensMorris.GameState
import com.kr8ne.mensMorris.Position
import com.kr8ne.mensMorris.gameStartPosition
import com.kr8ne.mensMorris.move.Movement
import com.kroune.NetworkResponse
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class GameData(val firstUser: Connection, val secondUser: Connection) {
    private var position: Position = gameStartPosition
    val isFirstPlayerGreen = Random.nextBoolean()

    private fun getPosition(): String {
        val result = Json.encodeToString<Position>(position)
        println(result)
        return result
    }


    suspend fun sendMove(jwtToken: CustomJwtToken, movement: Movement, opposite: Boolean) {
        val move = Json.encodeToString<Movement>(movement)
        when {
            firstUser.jwtToken == jwtToken -> {
                val user = if (opposite) secondUser else firstUser
                user.session.send(move)
            }

            secondUser.jwtToken == jwtToken -> {
                val user = if (opposite) firstUser else secondUser
                user.session.send(move)
            }

            else -> {
                error("")
            }
        }
        println(move)
    }

    suspend fun sendPosition(jwtToken: CustomJwtToken, opposite: Boolean) {
        val pos = getPosition()
        when {
            firstUser.jwtToken == jwtToken -> {
                val user = if (opposite) secondUser else firstUser
                user.session.send(pos)
            }

            secondUser.jwtToken == jwtToken -> {
                val user = if (opposite) firstUser else secondUser
                user.session.send(pos)
            }

            else -> {
                error("")
            }
        }
        println(NetworkResponse(200, pos).encode())
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

    fun applyMove(move: Movement) {
        position = move.producePosition(position)
    }

    fun hasEnded(): Boolean {
        return position.gameState() == GameState.End
    }


    fun isParticipating(jwtToken: CustomJwtToken): Boolean {
        return firstUser.jwtToken == jwtToken || secondUser.jwtToken == jwtToken
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

//{"startIndex":null,"endIndex":5}
class Connection(var jwtToken: CustomJwtToken, var session: DefaultWebSocketSession)

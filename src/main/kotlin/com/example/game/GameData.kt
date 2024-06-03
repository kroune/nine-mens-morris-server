package com.example.game

import com.kr8ne.mensMorris.GameState
import com.kr8ne.mensMorris.Position
import com.kr8ne.mensMorris.gameStartPosition
import com.kr8ne.mensMorris.move.Movement
import com.kroune.MoveResponse
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class GameData(val firstUser: Connection, val secondUser: Connection) {
    private var position: Position = gameStartPosition
    val isFirstPlayerGreen = Random.nextBoolean()

    private fun getPosition(): String {
        val result = Json.encodeToString(position)
        println(result)
        return result
    }

    suspend fun sendPosition(jwtToken: String, opposite: Boolean) {
        if (opposite) {
            if (firstUser.jwtToken == jwtToken) {
                secondUser.session.send(MoveResponse(200, getPosition()).encode())
            }
            if (secondUser.jwtToken == jwtToken) {
                firstUser.session.send(MoveResponse(200, getPosition()).encode())
            }
        } else {
            if (firstUser.jwtToken == jwtToken) {
                firstUser.session.send(MoveResponse(200, getPosition()).encode())
            }
            if (secondUser.jwtToken == jwtToken) {
                secondUser.session.send(MoveResponse(200, getPosition()).encode())
            }
        }
    }

    fun isValidMove(move: Movement): Boolean {
        return position.generateMoves(0u, true).contains(move)
    }

    fun applyMove(move: Movement) {
        position = move.producePosition(position)
    }

    fun hasEnded(): Boolean {
        return position.gameState() == GameState.End
    }


    fun isValidJwtToken(jwtToken: String): Boolean {
        return firstUser.jwtToken == jwtToken || secondUser.jwtToken == jwtToken
    }

    fun updateSession(jwtToken: String, session: DefaultWebSocketServerSession) {
        if (firstUser.jwtToken == jwtToken) {
            firstUser.session = session
        }
        if (secondUser.jwtToken == jwtToken) {
            secondUser.session = session
        }
    }
}

//{"startIndex":null,"endIndex":5}
class Connection(var jwtToken: String, var session: DefaultWebSocketSession)

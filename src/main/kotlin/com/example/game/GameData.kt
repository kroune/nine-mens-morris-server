package com.example.game

import com.kr8ne.mensMorris.GameState
import com.kr8ne.mensMorris.Position
import com.kr8ne.mensMorris.gameStartPosition
import com.kr8ne.mensMorris.move.Movement
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameData(private val firstUser: Connection, private val secondUser: Connection) {
    private var position: Position = gameStartPosition
    fun getPosition(): String {
        val pos = PositionAdapter(position.positions, position.freePieces, position.pieceToMove, position.removalCount)
        val result = Json.encodeToString(pos)
        println(result)
        return result
    }

    fun applyMove(move: Movement) {
        position = move.producePosition(position)
    }

    suspend fun notifyAboutChanges() {
        firstUser.session.send(getPosition())
        secondUser.session.send(getPosition())
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


@Serializable
data class PositionAdapter(
    @Serializable
    val positions: Array<Boolean?>,
    @Serializable
    val freePieces: Pair<UByte, UByte> = Pair(0U, 0U),
    @Serializable
    val pieceToMove: Boolean,
    @Serializable
    val removalCount: Byte = 0
)

@Serializable
data class MovementAdapter(
    @Serializable
    val startIndex: Int?,
    @Serializable
    val endIndex: Int?
)

fun MovementAdapter.toMovement(): Movement {
    return Movement(startIndex, endIndex)
}

//{"startIndex":null,"endIndex":5}
class Connection(var jwtToken: String, var session: DefaultWebSocketSession)

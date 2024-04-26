package com.example

import com.kr8ne.mensMorris.GameState
import com.kr8ne.mensMorris.Position
import com.kr8ne.mensMorris.gameStartPosition
import com.kr8ne.mensMorris.move.Movement
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameData() {
    var position: Position = gameStartPosition
    fun getPosition(): String {
        val pos = PositionAdapter(position.positions, position.freePieces, position.pieceToMove, position.removalCount)
        val result = Json.encodeToString(pos)
        return result
    }

    fun applyMove(move: Movement) {
        position = move.producePosition(position)
    }

    fun hasEnded(): Boolean {
        return position.gameState() == GameState.End
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

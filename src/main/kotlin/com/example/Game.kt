package com.example

import com.kr8ne.mensMorris.Position
import com.kr8ne.mensMorris.gameStartPosition
import com.kr8ne.mensMorris.move.Movement
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Game(val ip1: String, val ip2: String) {
    fun getPosition(): String {
        val pos = PositionShare(position.positions, position.freePieces, position.pieceToMove, position.removalCount)
        return Json.encodeToString(pos)
    }

    fun applyMove(move: Movement, ip: String): Response {
        if (!canPerformMove(ip)) {
            return Response.IllegalTiming
        }
        if (!position.generateMoves(0u, true).contains(move)) {
            return Response.IllegalMove
        }
        position = move.producePosition(position)
        return Response.Success
    }

    private fun canPerformMove(ip: String): Boolean {
        if (position.pieceToMove && ip == ip1) {
            return true
        }
        if (!position.pieceToMove && ip == ip2) {
            return true
        }
        return false
    }

    var position: Position = gameStartPosition
}

enum class Response {
    IllegalTiming,
    IllegalMove,
    Success
}

@Serializable
data class PositionShare(
    @Serializable
    var positions: Array<Boolean?>,
    @Serializable
    var freePieces: Pair<UByte, UByte> = Pair(0U, 0U),
    @Serializable
    var pieceToMove: Boolean,
    @Serializable
    var removalCount: Byte = 0
)

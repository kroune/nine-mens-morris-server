package gameMain.data.dao.movesHistory

import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.move.Movement

data class MovesHistoryStep(
    val move: Movement,
    val position: Position,
    val moveNumber: Int,
    val player: Long,
    val time: Long,
)
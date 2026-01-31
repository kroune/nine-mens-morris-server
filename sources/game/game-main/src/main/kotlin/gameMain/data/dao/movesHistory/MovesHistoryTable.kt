package gameMain.data.dao.movesHistory

import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.move.Movement
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.json
import userApi.data.UsersDataTable

object MovesHistoryTable : Table("moves_history") {
    val gameId = long("game_id").index()
    val player = reference("player", UsersDataTable.id)
    val time = long("time")
    val moveNumber = integer("move_number").index()
    val move = json<Movement>("move", Json.Default)
    val position = json<Position>("position", Json.Default)
}
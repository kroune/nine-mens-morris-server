package gameMain.data.dao.gameData

import botsApi.BotsDataTable
import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.gameStartPosition
import com.kroune.nineMensMorrisShared.GameEndReason
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.json
import userApi.data.UsersDataTable

object GamesDataTable : Table("games_data") {
    val gameId = long("game_id").autoIncrement().uniqueIndex()
    val firstPlayer = reference("first_player", UsersDataTable.id).index()
    val secondPlayer = reference("second_player", UsersDataTable.id).index()
    val botId = reference("bot_id", BotsDataTable.userId).nullable()
    val position = json<Position>("position", Json.Default).default(gameStartPosition)
    val movesCount = integer("moves_count").default(0)
    val firstPlayerMovesFirst = bool("first_player_moves_first")
    val gameEndReason = json<GameEndReason>("game_end_reason", Json.Default).nullable().default(null)

    override val primaryKey = PrimaryKey(gameId)
}
package com.example.data.users

import com.example.data.bots.BotsDataTable
import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.gameStartPosition
import com.kroune.nineMensMorrisLib.move.Movement
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.json

object GamesDataTable : Table("games_data") {
    val gameId = long("game_id").autoIncrement().uniqueIndex()
    val firstPlayer = reference("first_player", UsersDataTable.id).uniqueIndex()
    val secondPlayer = reference("second_player", UsersDataTable.id).uniqueIndex()
    val botId = reference("bot_id", BotsDataTable.userId).nullable()
    val position = json<Position>("position", Json).default(gameStartPosition)
    val moveHistory = json<List<Movement>>("moves_history", Json).default(listOf())
    val firstPlayerMovesFirst = bool("first_player_moves_first")
    val movesCount = integer("moves_count").default(0)

    override val primaryKey = PrimaryKey(gameId)
}

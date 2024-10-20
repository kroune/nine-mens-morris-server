/*
 * This file is part of nine-mens-morris-server (https://github.com/kroune/nine-mens-morris-server)
 * Copyright (C) 2024-2024  kroune
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact: kr0ne@tuta.io
 */
package com.example.data.local.games

import com.example.data.local.bots.BotsDataTable
import com.example.data.local.users.UsersDataTable
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

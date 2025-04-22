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
package gameMain.data.dao

import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.move.Movement

interface GamesDataServiceI {
    /**
     * Tries to create game with provided parameters
     * @param game game information
     * @return true if game was created, false if not (such game already exists)
     */
    suspend fun create(game: GameData): Boolean
    suspend fun getPositionByGameId(gameId: Long): Position?
    suspend fun getGameMoveHistory(gameId: Long): List<Movement>?
    suspend fun applyMove(gameId: Long, move: Movement)
    suspend fun getFirstUserIdByGameId(gameId: Long): Long?
    suspend fun getSecondUserIdByGameId(gameId: Long): Long?
    suspend fun getFirstPlayerMovesFirstByGameId(gameId: Long): Boolean?

    /**
     * @param gameId id of the game
     * @return null if there is no bot in game or botId
     */
    suspend fun getBotIdByGameId(gameId: Long): Long?
    suspend fun getMovesCountByGameId(gameId: Long): Int?
    suspend fun getGameIdByUserId(userId: Long): Long?
    suspend fun participates(userId: Long): Boolean
    suspend fun exists(gameId: Long): Boolean
    suspend fun delete(gameId: Long)
}
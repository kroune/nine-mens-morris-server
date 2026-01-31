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

package gameCommon

import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlinx.serialization.SerializationException

abstract class GameI {
    abstract val gameId: Long
    abstract var firstPlayer: DefaultWebSocketServerSession?
    abstract var secondPlayer: DefaultWebSocketServerSession?

    abstract suspend fun initializeGame()

    abstract fun timeoutCheck(previousMoveCount: Int?)

    /**
     * takes actions when game has ended
     * @param reason - reason why the game has ended
     */
    abstract fun handleGameEnd(reason: GameEndReason)

    abstract suspend fun sendMove(userId: Long, movement: Movement, opposite: Boolean)

    @Throws(SerializationException::class, IllegalArgumentException::class)
    abstract suspend fun sendPosition(session: DefaultWebSocketServerSession)

    abstract suspend fun isMovePossible(move: Movement, userId: Long): Boolean

    abstract suspend fun applyMove(move: Movement, isFirstPlayerPerformedMove: Boolean, userId: Long)
    abstract suspend fun isFirstPlayerMovesFirst(): Boolean
    abstract fun isFirstPlayer(userId: Long): Boolean
    abstract fun updateSession(userId: Long, session: DefaultWebSocketServerSession)
    abstract fun enemyId(userId: Long): Long
}
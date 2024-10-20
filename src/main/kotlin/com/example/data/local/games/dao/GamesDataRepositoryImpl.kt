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
package com.example.data.local.games.dao

import com.example.data.local.games.GameData
import com.example.data.local.games.GamesDataTable
import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.move.Movement
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class GamesDataRepositoryImpl : GamesDataRepositoryI {
    init {
        transaction {
            SchemaUtils.create(GamesDataTable)
        }
    }

    override suspend fun create(game: GameData) {
        newSuspendedTransaction {
            GamesDataTable.insert {
                it[firstPlayer] = game.firstPlayerId
                it[secondPlayer] = game.secondPlayerId
                it[botId] = game.botId
                it[position] = game.position
                it[moveHistory] = game.movesHistory
                it[firstPlayerMovesFirst] = game.firstPlayerMovesFirst
                it[movesCount] = game.movesCount
            }
        }
    }

    override suspend fun getPositionByGameId(gameId: Long): Position? {
        return newSuspendedTransaction {
            GamesDataTable.selectAll().where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.position]
            }.firstOrNull()
        }
    }

    override suspend fun getGameMoveHistory(gameId: Long): List<Movement>? {
        return newSuspendedTransaction {
            GamesDataTable.selectAll().where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.moveHistory]
            }.firstOrNull()
        }
    }

    override suspend fun applyMove(gameId: Long, move: Movement) {
        newSuspendedTransaction {
            val newMoveHistory = getGameMoveHistory(gameId)!!.toMutableList().apply { add(move) }
            val newGamePosition = move.producePosition(getPositionByGameId(gameId)!!)
            GamesDataTable.update(
                { GamesDataTable.gameId eq gameId }
            ) {
                it[position] = newGamePosition
                it[moveHistory] = newMoveHistory
            }
        }
    }

    override suspend fun getBotIdByGameId(gameId: Long): Long? {
        return newSuspendedTransaction {
            GamesDataTable.selectAll().where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.botId]
            }.firstOrNull()
        }
    }

    override suspend fun getFirstPlayerMovesFirstByGameId(gameId: Long): Boolean? {
        return newSuspendedTransaction {
            GamesDataTable.selectAll().where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.firstPlayerMovesFirst]
            }.firstOrNull()
        }
    }

    override suspend fun getFirstUserIdByGameId(gameId: Long): Long? {
        return newSuspendedTransaction {
            GamesDataTable.selectAll().where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.firstPlayer]
            }.firstOrNull()
        }
    }

    override suspend fun getSecondUserIdByGameId(gameId: Long): Long? {
        return newSuspendedTransaction {
            GamesDataTable.selectAll().where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.secondPlayer]
            }.firstOrNull()
        }
    }

    override suspend fun getMovesCountByGameId(gameId: Long): Int? {
        return newSuspendedTransaction {
            GamesDataTable.selectAll().where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.movesCount]
            }.firstOrNull()
        }
    }

    override suspend fun getGameIdByUserId(userId: Long): Long? {
        return newSuspendedTransaction {
            GamesDataTable.selectAll().where {
                (GamesDataTable.firstPlayer eq userId) or (GamesDataTable.secondPlayer eq userId)
            }.limit(1).map {
                it[GamesDataTable.gameId]
            }.firstOrNull()
        }
    }

    override suspend fun participates(userId: Long): Boolean {
        return newSuspendedTransaction {
            GamesDataTable.selectAll().where {
                (GamesDataTable.firstPlayer eq userId) or (GamesDataTable.secondPlayer eq userId)
            }.limit(1).map {
                it[GamesDataTable.gameId]
            }.any()
        }
    }

    override suspend fun exists(gameId: Long): Boolean {
        return newSuspendedTransaction {
            GamesDataTable.selectAll().where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.gameId]
            }.any()
        }
    }

    override suspend fun delete(gameId: Long) {
        newSuspendedTransaction {
            GamesDataTable.deleteWhere {
                GamesDataTable.gameId eq gameId
            }
        }
    }
}
package gameMain.data.dao.gameData

import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.move.Movement
import com.kroune.nineMensMorrisShared.GameEndReason
import gameCommon.data.dao.GameData
import gameCommon.data.dao.GamesDataServiceI
import gameMain.data.dao.movesHistory.MovesHistoryTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

internal class GamesDataServiceImpl : GamesDataServiceI {
    init {
        transaction {
            SchemaUtils.create(GamesDataTable)
            SchemaUtils.create(MovesHistoryTable)
        }
    }

    override suspend fun create(game: GameData): Boolean {
        return newSuspendedTransaction {
            val usersFree = GamesDataTable.select(GamesDataTable.gameId).where {
                ((GamesDataTable.firstPlayer eq game.firstPlayerId) or
                        (GamesDataTable.secondPlayer eq game.secondPlayerId)) and GamesDataTable.gameEndReason.isNull()
            }.empty()
            if (!usersFree) {
                return@newSuspendedTransaction false
            }
            GamesDataTable.insert {
                it[firstPlayer] = game.firstPlayerId
                it[secondPlayer] = game.secondPlayerId
                it[botId] = game.botId
                it[position] = game.position
                it[firstPlayerMovesFirst] = game.firstPlayerMovesFirst
            }
            true
        }
    }

    override suspend fun getPositionByGameId(gameId: Long): Position? {
        return newSuspendedTransaction {
            GamesDataTable.select(GamesDataTable.position).where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.position]
            }.firstOrNull()
        }
    }

    override suspend fun applyMove(gameId: Long, move: Movement, userId: Long) {
        val newGamePosition = move.producePosition(getPositionByGameId(gameId)!!)
        val movesCount = getMovesCountByGameId(gameId)!!
        newSuspendedTransaction {
            MovesHistoryTable.insert {
                it[MovesHistoryTable.gameId] = gameId
                it[MovesHistoryTable.player] = userId
                it[MovesHistoryTable.time] = System.currentTimeMillis()
                it[MovesHistoryTable.moveNumber] = movesCount + 1
                it[MovesHistoryTable.move] = move
                it[MovesHistoryTable.position] = newGamePosition
            }
        }
        newSuspendedTransaction {
            GamesDataTable.update(
                { GamesDataTable.gameId eq gameId }
            ) {
                it[GamesDataTable.position] = newGamePosition
                it[GamesDataTable.movesCount] = movesCount + 1
            }
        }
    }

    override suspend fun getBotIdByGameId(gameId: Long): Long? {
        return newSuspendedTransaction {
            GamesDataTable.select(GamesDataTable.botId).where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.botId]
            }.firstOrNull()
        }
    }

    override suspend fun getFirstPlayerMovesFirstByGameId(gameId: Long): Boolean? {
        return newSuspendedTransaction {
            GamesDataTable.select(GamesDataTable.firstPlayerMovesFirst).where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.firstPlayerMovesFirst]
            }.firstOrNull()
        }
    }

    override suspend fun getFirstUserIdByGameId(gameId: Long): Long? {
        return newSuspendedTransaction {
            GamesDataTable.select(GamesDataTable.firstPlayer).where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.firstPlayer]
            }.firstOrNull()
        }
    }

    override suspend fun getSecondUserIdByGameId(gameId: Long): Long? {
        return newSuspendedTransaction {
            GamesDataTable.select(GamesDataTable.secondPlayer).where {
                GamesDataTable.gameId eq gameId
            }.limit(1).map {
                it[GamesDataTable.secondPlayer]
            }.firstOrNull()
        }
    }

    override suspend fun getMovesCountByGameId(gameId: Long): Int? {
        return newSuspendedTransaction {
            GamesDataTable.select(GamesDataTable.movesCount)
                .where {
                    (GamesDataTable.gameId eq gameId) and GamesDataTable.gameEndReason.isNull()
                }
                .limit(1)
                .map {
                    it[GamesDataTable.movesCount]
                }.firstOrNull()
        }
    }

    override suspend fun getGameIdByUserId(userId: Long): Long? {
        return newSuspendedTransaction {
            GamesDataTable.select(GamesDataTable.gameId).where {
                (GamesDataTable.firstPlayer eq userId) or (GamesDataTable.secondPlayer eq userId) and (GamesDataTable.gameEndReason.isNull())
            }.limit(1).map {
                it[GamesDataTable.gameId]
            }.firstOrNull()
        }
    }

    override suspend fun participatesInExistingGame(gameId: Long, userId: Long): Boolean {
        return newSuspendedTransaction {
            GamesDataTable.select(GamesDataTable.gameId).where {
                ((GamesDataTable.firstPlayer eq userId) or (GamesDataTable.secondPlayer eq userId)) and
                        (GamesDataTable.gameId eq gameId) and (GamesDataTable.gameEndReason.isNull())
            }.limit(1).map {
                it[GamesDataTable.gameId]
            }.any()
        }
    }

    override suspend fun exists(gameId: Long): Boolean {
        return newSuspendedTransaction {
            GamesDataTable.select(GamesDataTable.gameId).where {
                GamesDataTable.gameId eq gameId and (GamesDataTable.gameEndReason.isNull())
            }.limit(1).map {
                it[GamesDataTable.gameId]
            }.any()
        }
    }

    override suspend fun markGameAsDeleted(gameId: Long, gameEndReason: GameEndReason) {
        newSuspendedTransaction {
            GamesDataTable.update(
                { GamesDataTable.gameId eq gameId }
            ) {
                it[GamesDataTable.gameEndReason] = gameEndReason
            }
        }
    }
}
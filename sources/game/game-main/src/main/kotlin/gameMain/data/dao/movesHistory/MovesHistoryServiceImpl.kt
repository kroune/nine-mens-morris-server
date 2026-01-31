package gameMain.data.dao.movesHistory

import gameMain.data.dao.movesHistory.MovesHistoryTable.moveNumber
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class MovesHistoryServiceImpl: MovesHistoryServiceI {
    init {
        transaction {
            SchemaUtils.create(MovesHistoryTable)
        }
    }

    override suspend fun fetchMoveHistory(gameId: Long): List<MovesHistoryStep> {
        return newSuspendedTransaction {
            MovesHistoryTable.selectAll().where {
                (MovesHistoryTable.gameId eq gameId) and (MovesHistoryTable.moveNumber eq moveNumber)
            }.orderBy(MovesHistoryTable.moveNumber).map {
                MovesHistoryStep(
                    it[MovesHistoryTable.move],
                    it[MovesHistoryTable.position],
                    it[MovesHistoryTable.moveNumber],
                    it[MovesHistoryTable.player],
                    it[MovesHistoryTable.time]
                )
            }
        }
    }
}
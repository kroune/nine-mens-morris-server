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
package gameQueue.data.queue.dao

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import gameQueue.data.queue.QueueTable

internal class QueueServiceImpl : QueueServiceI {
    init {
        transaction {
            SchemaUtils.create(QueueTable)
        }
    }

    override suspend fun addUser(userId: Long, bucketRange: IntRange) {
        newSuspendedTransaction(transactionIsolation = 2) {
            // delete all other matches
            QueueTable.deleteWhere { QueueTable.userId eq userId }
            bucketRange.forEach { bucket ->
                QueueTable.insert {
                    it[bucketId] = bucket
                    it[QueueTable.userId] = userId
                }
            }
        }
    }

    override suspend fun getUsers(bucket: Int): List<Long> {
        return newSuspendedTransaction {
            QueueTable.select(QueueTable.userId).where {
                QueueTable.bucketId eq bucket
            }.map {
                it[QueueTable.userId]
            }
        }
    }

    override suspend fun deleteUser(userId: Long): Int {
        return newSuspendedTransaction(transactionIsolation = 2) {
            QueueTable.deleteWhere {
                QueueTable.userId eq userId
            }
        }
    }
}
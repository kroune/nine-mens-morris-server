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
package botsImpl.dao

import botsApi.dao.BotsServiceI
import botsApi.BotsDataTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

internal class BotsServiceImpl(
    private val database: Database
) : BotsServiceI {
    init {
        transaction {
            SchemaUtils.create(BotsDataTable)
        }
    }

    override suspend fun add(id: Long) {
        newSuspendedTransaction(db = database) {
            BotsDataTable.insert {
                it[userId] = id
            }
        }
    }

    override suspend fun exists(id: Long): Boolean {
        return newSuspendedTransaction(db = database) {
            BotsDataTable.select(BotsDataTable.userId).where {
                BotsDataTable.userId eq id
            }.limit(1).map {
                it[BotsDataTable.userId]
            }.any()
        }
    }
}
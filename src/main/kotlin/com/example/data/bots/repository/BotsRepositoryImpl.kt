package com.example.data.bots.repository

import com.example.data.bots.BotsDataTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class BotsRepositoryImpl: BotsRepositoryI {
    init {
        transaction {
            SchemaUtils.create(BotsDataTable)
        }
    }

    override suspend fun add(id: Long) {
        newSuspendedTransaction {
            BotsDataTable.insert {
                it[userId] = id
            }
        }
    }

    override suspend fun get(id: Long): Boolean {
        return newSuspendedTransaction {
            BotsDataTable.selectAll().where {
                BotsDataTable.userId eq id
            }.limit(1).map {
                it[BotsDataTable.userId]
            }.any()
        }
    }
}
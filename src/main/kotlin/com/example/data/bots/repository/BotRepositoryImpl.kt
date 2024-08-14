package com.example.data.bots.repository

import com.example.data.bots.BotDataTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class BotRepositoryImpl: BotRepositoryI {
    init {
        transaction {
            SchemaUtils.create(BotDataTable)
        }
    }

    override suspend fun add(id: Long) {
        newSuspendedTransaction {
            BotDataTable.insert {
                it[userId] = id
            }
        }
    }

    override suspend fun get(id: Long): Boolean {
        return newSuspendedTransaction {
            BotDataTable.selectAll().where {
                BotDataTable.userId eq id
            }.limit(1).map {
                it[BotDataTable.userId]
            }.any()
        }
    }
}
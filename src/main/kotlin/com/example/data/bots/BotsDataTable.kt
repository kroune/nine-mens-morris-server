package com.example.data.bots

import com.example.data.users.UsersDataTable
import org.jetbrains.exposed.sql.Table

object BotsDataTable: Table("bot_data") {
    val userId = reference("user_id", UsersDataTable.id).uniqueIndex()
}
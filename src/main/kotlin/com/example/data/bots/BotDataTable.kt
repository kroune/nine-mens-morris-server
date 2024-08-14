package com.example.data.bots

import com.example.data.users.UsersDataTable
import org.jetbrains.exposed.sql.Table

object BotDataTable: Table("bot_data") {
    val userId = reference("user_id", UsersDataTable.id)
}
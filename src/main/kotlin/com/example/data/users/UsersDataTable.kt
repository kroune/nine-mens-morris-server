package com.example.data.users

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date

object UsersDataTable : Table("users_data") {
    val login = varchar("login", 20)
    val passwordHash = varchar("password_hash", 500)
    val id = long("id").autoIncrement().uniqueIndex()
    val rating = integer("rating").default(1000)
    val creationDate = date("creation_date")
    val profilePicture = binary("profile_picture").nullable()

    override val primaryKey = PrimaryKey(id)
}

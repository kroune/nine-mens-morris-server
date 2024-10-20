package com.example

import com.example.data.local.users.UserData
import com.example.data.local.usersRepository
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer

object TestDatabase {
    private val mySQLContainer: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:16").apply {
        withDatabaseName("test-db")
        withUsername("test-user")
        withPassword("test-password")
        start() // Start the container
    }

    fun createDummyUser(userData: UserData = UserData("testLogin", "testPassword")): UserData {
        runBlocking {
            usersRepository.create(userData)
        }
        return userData
    }

    fun connect() {
        Database.connect(
            mySQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = mySQLContainer.username,
            password = mySQLContainer.password
        )
    }
}
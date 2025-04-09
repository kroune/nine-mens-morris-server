package io.github.kroune

import io.github.kroune.data.local.botsRepository
import io.github.kroune.data.local.gamesRepository
import io.github.kroune.data.local.queueRepository
import io.github.kroune.data.local.users.InsertUserData
import io.github.kroune.data.local.usersRepository
import io.github.kroune.features.ConfigurationLoader.currentConfig
import io.github.kroune.features.encryption.JwtTokenImpl
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.reflect.full.createInstance

class TestDatabase {
    private var mySQLContainer: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:17").apply {
        withDatabaseName("test-db")
        withUsername("test-user")
        withPassword("test-password")
        start() // Start the container
    }

    fun createDummyUser(userData: InsertUserData = InsertUserData("testLogin", "testPassword")): Pair<InsertUserData, String> {
        val jwtToken = runBlocking {
            usersRepository.create(userData)
            JwtTokenImpl(userData.login, userData.password).token
        }
        return userData to jwtToken
    }

    fun connect() {
        val previousValue = currentConfig.gameConfig.maxBucketNumber
        val maxBucketNumber = currentConfig.gameConfig::class.java.getDeclaredField("maxBucketNumber")
        maxBucketNumber.isAccessible = true
        maxBucketNumber.set(currentConfig.gameConfig, -1)
        Database.connect(
            mySQLContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = mySQLContainer.username,
            password = mySQLContainer.password
        )

        // beautiful, isn't it?
        println("creating tables")
        usersRepository = usersRepository::class.createInstance()
        gamesRepository = gamesRepository::class.createInstance()
        botsRepository = botsRepository::class.createInstance()
        queueRepository = queueRepository::class.createInstance()
        println("created tables")
        maxBucketNumber.set(currentConfig.gameConfig, previousValue)
    }
}
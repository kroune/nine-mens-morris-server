package commonTests

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext
import org.testcontainers.containers.PostgreSQLContainer

object TestDatabase : KoinComponent {
    var pgContainer: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:17").apply {
        withDatabaseName("test-db")
        withUsername("test-user")
        withPassword("test-password")
        start() // Start the container
    }

    var database: Database? = null

    fun create() {
        val newPgContainer = PostgreSQLContainer<Nothing>("postgres:17").apply {
            withDatabaseName("test-db")
            withUsername("test-user")
            withPassword("test-password")
            start() // Start the container
        }
        println("connecting to db")
        val newDatabase = Database.connect(
            newPgContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = newPgContainer.username,
            password = newPgContainer.password
        )
        database?.let {
            TransactionManager.closeAndUnregister(it)
        }
        pgContainer.stop()
        pgContainer = newPgContainer
        database = newDatabase
        GlobalContext.get().declare(newPgContainer)
    }
}

package data.dao

import data.VersionDataTable
import model.Distribution
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class VersionDataServiceImpl(
    private val database: Database
) : VersionDataServiceI {
    init {
        transaction {
            SchemaUtils.create(VersionDataTable)
        }
    }

    override suspend fun lastVersion(distribution: Distribution): Int? {
        return newSuspendedTransaction(db = database) {
            VersionDataTable
                .selectAll()
                .where {
                    (VersionDataTable.distribution eq distribution)
                }
                .orderBy(VersionDataTable.version)
                .limit(1)
                .map { it[VersionDataTable.version] }
                .singleOrNull()
        }
    }

    override suspend fun requiredVersion(version: Int, distribution: Distribution): Int? {
        return newSuspendedTransaction(db = database) {
            VersionDataTable
                .select(VersionDataTable.version)
                .where {
                    (VersionDataTable.distribution eq distribution) and
                            (VersionDataTable.version greater version) and
                            (VersionDataTable.breakingChanges eq true)
                }
                .orderBy(VersionDataTable.version)
                .limit(1)
                .map {
                    it[VersionDataTable.version]
                }
                .singleOrNull()
        }
    }

    override suspend fun addVersion(version: Int, distribution: Distribution, breakingChanges: Boolean) {
        newSuspendedTransaction(db = database) {
            VersionDataTable
                .insert {
                    it[VersionDataTable.version] = version
                    it[VersionDataTable.breakingChanges] = breakingChanges
                    it[VersionDataTable.distribution] = distribution
                }
        }
    }
}
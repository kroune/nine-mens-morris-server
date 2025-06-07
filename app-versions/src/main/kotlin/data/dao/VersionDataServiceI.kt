package data.dao

import model.Distribution

interface VersionDataServiceI {
    suspend fun lastVersion(distribution: Distribution): Int?
    suspend fun requiredVersion(version: Int, distribution: Distribution): Int?
    suspend fun addVersion(version: Int, distribution: Distribution, breakingChanges: Boolean)
}
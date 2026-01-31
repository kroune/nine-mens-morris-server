package appVersions.data

import appVersions.model.Distribution
import org.jetbrains.exposed.sql.Table

/**
 * stores all app versions
 */
internal object VersionDataTable: Table("version_data") {
    val distribution = enumeration<Distribution>("distribution")
    val version = integer("version")
    val breakingChanges = bool("breaking_changes")
}
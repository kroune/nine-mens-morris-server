package appVersions.version.get

import appVersions.data.dao.VersionDataServiceI
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get
import appVersions.version.validateDistribution
import appVersions.version.validateVersion

fun Route.versionRoutingGET() {
    get("last-version") {
        val distribution = validateDistribution() ?: return@get
        val versionDataService = get<VersionDataServiceI>()
        val version = versionDataService.lastVersion(distribution)
        call.respondNullable(version)
    }
    get("required-version") {
        val version = validateVersion() ?: return@get
        val distribution = validateDistribution() ?: return@get
        val versionDataService = get<VersionDataServiceI>()
        val requiredVersion: Int? = versionDataService.requiredVersion(version, distribution)
        call.respondNullable(requiredVersion)
    }
}
package routing.version.get

import data.dao.VersionDataServiceI
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.get
import routing.version.validateDistribution
import routing.version.validateVersion

fun Route.versionRoutingGET() {
    get("last-version") {
        val distribution = validateDistribution() ?: return@get
        val versionDataService = get<VersionDataServiceI>()
        val version = versionDataService.lastVersion(distribution)
        val versionAsText = Json.encodeToString(version)
        call.respond(versionAsText)
    }
    get("required-version") {
        val version = validateVersion() ?: return@get
        val distribution = validateDistribution() ?: return@get
        val versionDataService = get<VersionDataServiceI>()
        val requiredVersion: Int? = versionDataService.requiredVersion(version, distribution)
        val versionAsText = Json.encodeToString(requiredVersion)
        call.respond(versionAsText)
    }
}
package routing.version.get

import data.dao.VersionDataServiceI
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Distribution
import org.koin.ktor.ext.get

fun Route.versionRoutingGET() {
    get("last-version") {
        val distribution = call.queryParameters["distribution"].let {
            if (it == null) {
                call.respond(HttpStatusCode.BadRequest, "no [distribution] parameter found")
                return@get
            }
            try {
                Distribution.valueOf(it)
            } catch (_: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "[distribution] parameter is not valid")
                return@get
            }
        }
        val versionDataService = get<VersionDataServiceI>()
        val version = versionDataService.lastVersion(distribution)
        val versionAsText = Json.encodeToString(version)
        call.respond(versionAsText)
    }
    get("required-version") {
        val version = call.queryParameters["version"].let {
            if (it == null) {
                call.respond(HttpStatusCode.BadRequest, "no [version] parameter found")
                return@get
            }
            val version = it.toIntOrNull()
            if (version == null) {
                call.respond(HttpStatusCode.BadRequest, "[version] parameter is not valid")
                return@get
            }
            version
        }
        val distributionString = call.queryParameters["distribution"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "no [distribution] parameter found")
            return@get
        }
        val versionDataService = get<VersionDataServiceI>()
        val distribution = try {
            Distribution.valueOf(distributionString)
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "[distribution] parameter is not valid")
            return@get
        }
        val requiredVersion: Int? = versionDataService.requiredVersion(version, distribution)
        val versionAsText = Json.encodeToString(requiredVersion)
        call.respond(versionAsText)
    }
}
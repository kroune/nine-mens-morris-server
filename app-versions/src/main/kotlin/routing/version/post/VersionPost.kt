package routing.version.post

import common.ConfigurationLoader
import data.dao.VersionDataServiceI
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.Distribution
import org.koin.ktor.ext.get

fun Route.versionRoutingPOST() {
    post("update-version") {
        call.queryParameters["token"].let {
            if (it == null) {
                call.respond(HttpStatusCode.BadRequest, "no [token] parameter found")
                return@post
            }
            val config = get<ConfigurationLoader.ConfigMember>()
            val isTokenActivated = config.versionUpdateConfig.tokens[it]
            if (isTokenActivated != true) {
                call.respond(HttpStatusCode.Unauthorized, "[token] parameter is not valid")
                return@post
            }
        }
        val version = call.queryParameters["version"].let {
            if (it == null) {
                call.respond(HttpStatusCode.BadRequest, "no [version] parameter found")
                return@post
            }
            val version = it.toIntOrNull()
            if (version == null) {
                call.respond(HttpStatusCode.BadRequest, "[version] parameter is not valid")
                return@post
            }
            version
        }
        val distribution = call.queryParameters["distribution"].let {
            if (it == null) {
                call.respond(HttpStatusCode.BadRequest, "no [distribution] parameter found")
                return@post
            }
            try {
                Distribution.valueOf(it)
            } catch (_: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "[distribution] parameter is not valid")
                return@post
            }
        }
        val breakingChanges = call.queryParameters["breaking_changes"].let {
            if (it == null) {
                call.respond(HttpStatusCode.BadRequest, "no [breaking_changes] parameter found")
                return@post
            }
            val breakingChanges = it.toBooleanStrictOrNull()
            if (breakingChanges == null) {
                call.respond(HttpStatusCode.BadRequest, "[breaking_changes] parameter is not valid")
                return@post
            }
            breakingChanges
        }
        val versionDataService = get<VersionDataServiceI>()
        versionDataService.addVersion(version, distribution, breakingChanges)
        call.respond(HttpStatusCode.OK)
    }
}
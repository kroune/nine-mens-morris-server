package appVersions.version.post

import common.ConfigurationLoader
import appVersions.data.dao.VersionDataServiceI
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get
import appVersions.version.validateDistribution
import appVersions.version.validateVersion

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
        val version = validateVersion() ?: return@post
        val distribution = validateDistribution() ?: return@post
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
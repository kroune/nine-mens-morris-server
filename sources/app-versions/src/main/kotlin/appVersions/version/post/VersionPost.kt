package appVersions.version.post

import appVersions.data.dao.VersionDataServiceI
import appVersions.version.validateDistribution
import appVersions.version.validateVersion
import common.ConfigurationLoader
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
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
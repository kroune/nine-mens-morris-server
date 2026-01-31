package appVersions.version

import appVersions.model.Distribution
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

internal suspend fun RoutingContext.validateDistribution(): Distribution? {
    call.queryParameters["distribution"].let {
        if (it == null) {
            call.respond(HttpStatusCode.BadRequest, "no [distribution] parameter found")
            return null
        }
        return try {
            Distribution.valueOf(it)
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "[distribution] parameter is not valid")
            null
        }
    }
}

internal suspend fun RoutingContext.validateVersion(): Int? {
    call.queryParameters["version"].let {
        if (it == null) {
            call.respond(HttpStatusCode.BadRequest, "no [version] parameter found")
            return null
        }
        val version = it.toIntOrNull()
        if (version == null) {
            call.respond(HttpStatusCode.BadRequest, "[version] parameter is not valid")
            return null
        }
        return version
    }
}
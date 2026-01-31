package io.github.kroune.server.routing

import appVersions.version.get.versionRoutingGET
import appVersions.version.post.versionRoutingPOST
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

internal fun Route.versionRouting() {
    route("/version") {
        versionRoutingGET()
        versionRoutingPOST()
    }
}
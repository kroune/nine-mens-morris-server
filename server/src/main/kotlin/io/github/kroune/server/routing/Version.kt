package io.github.kroune.server.routing

import io.ktor.server.routing.*
import appVersions.version.get.versionRoutingGET
import appVersions.version.post.versionRoutingPOST

fun Route.versionRouting() {
    versionRoutingGET()
    versionRoutingPOST()
}
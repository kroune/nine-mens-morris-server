package io.github.kroune.server.routing

import io.ktor.server.routing.*
import routing.version.get.versionRoutingGET
import routing.version.post.versionRoutingPOST

fun Route.versionRouting() {
    versionRoutingGET()
    versionRoutingPOST()
}
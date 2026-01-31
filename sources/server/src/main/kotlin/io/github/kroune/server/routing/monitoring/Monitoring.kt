package io.github.kroune.server.routing.monitoring

import io.github.kroune.server.routing.monitoring.get.monitoringRoutingGET
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route

internal fun Route.monitoringRouting() {
    authenticate("prometheus") {
        monitoringRoutingGET()
    }
}
package io.github.kroune.routing.monitoring

import io.github.kroune.routing.monitoring.get.monitoringRoutingGET
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route

fun Route.monitoringRouting() {
    authenticate("prometheus") {
        monitoringRoutingGET()
    }
}
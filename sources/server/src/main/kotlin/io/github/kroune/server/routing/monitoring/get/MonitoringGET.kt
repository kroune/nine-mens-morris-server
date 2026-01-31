package io.github.kroune.server.routing.monitoring.get

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.ktor.ext.inject

internal fun Route.monitoringRoutingGET() {
    get("/metrics") {
        val registry by inject<PrometheusMeterRegistry>()
        call.respondText { registry.scrape() }
    }
}

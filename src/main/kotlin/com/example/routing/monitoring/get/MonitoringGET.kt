package com.example.routing.monitoring.get

import com.example.features.ConfigurationLoader.currentConfig
import io.ktor.server.request.header
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.koin.ktor.ext.inject

fun Route.monitoringRoutingGET() {
    get("/metrics") {
        // TODO: crappy code, but at least some security
        if (call.request.header("auth-token") != currentConfig.grafanaConfig.passwordForScrape)
            return@get
        val registry by inject<PrometheusMeterRegistry>()
        call.respondText { registry.scrape() }
    }
}

/*
 * This file is part of nine-mens-morris-server (https://github.com/kroune/nine-mens-morris-server)
 * Copyright (C) 2024-2024  kroune
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact: kr0ne@tuta.io
 */
package io.github.kroune

import io.github.kroune.data.local.botsRepository
import io.github.kroune.data.local.gamesRepository
import io.github.kroune.data.local.queueRepository
import io.github.kroune.data.local.usersRepository
import io.github.kroune.di.koinModules
import io.github.kroune.features.ConfigurationLoader.currentConfig
import io.github.kroune.features.logging.logger
import io.github.kroune.routing.auth.accountRouting
import io.github.kroune.routing.game.gameRouting
import io.github.kroune.routing.misc.miscRouting
import io.github.kroune.routing.monitoring.monitoringRouting
import io.github.kroune.routing.userInfo.userInfoRouting
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.GlobalContext
import org.koin.ktor.plugin.Koin
import kotlin.time.Duration.Companion.seconds

fun main() {
    val serverConfig = currentConfig.serverConfig
    embeddedServer(
        Netty,
        configure = {
            connector {
                host = serverConfig.host
                port = serverConfig.port
            }
            requestReadTimeoutSeconds = 15
            responseWriteTimeoutSeconds = 15
        },
        module = {
            startDI()
            logger.info { "starting server" }
            applyPlugins()
            installMonitoring()
            routing()
            database()
        }
    ).start(wait = true)
}

fun Application.startDI() {
    install(Koin) {
        modules(
            koinModules
        )
    }
}

fun Application.database() {
    currentConfig.serviceLocator.postgres.let {
        Database.connect(
            it.url,
            driver = "org.postgresql.Driver",
            user = it.username,
            password = it.password
        )
    }
    logger.debug { "initializing users repository" }
    usersRepository
    logger.debug { "initializing games repository" }
    gamesRepository
    logger.debug { "initializing bots repository" }
    botsRepository
    logger.debug { "initializing queue repository" }
    queueRepository
    logger.debug { "applying configs" }
}

fun Application.installMonitoring() {
    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        meterBinders = listOf(
            UptimeMetrics(),
            ProcessorMetrics(),

            ClassLoaderMetrics(),
            JvmCompilationMetrics(),
            JvmGcMetrics(),
            JvmHeapPressureMetrics(),
            JvmInfoMetrics(),
            JvmMemoryMetrics(),
            JvmThreadDeadlockMetrics(),
            JvmThreadMetrics(),
        )
        registry = appMicrometerRegistry
    }
    GlobalContext.getKoinApplicationOrNull()!!.koin.declare(appMicrometerRegistry)
}

fun Application.applyPlugins(includeRateLimitPlugin: Boolean = true) {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHost("github.io", listOf("http", "https"), listOf("kroune"))
        allowHost("nine-men-s-morris.me", listOf("http", "https"), listOf("play", "dev"))
    }
    install(Authentication) {
        basic("prometheus") {
            realm = "Access to the '/metrics' path"
            validate { credentials ->
                with(currentConfig.grafanaConfig) {
                    return@validate credentials.name == nameForScrape && credentials.password == passwordForScrape
                }
            }
        }
    }
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        val webSocketConfig = currentConfig.webSocketConfig
        pingPeriod = webSocketConfig.pingPeriod
        timeout = webSocketConfig.timeout
    }
    if (includeRateLimitPlugin)
        install(RateLimit) {
            global {
                val rateLimitConfig = currentConfig.rateLimitConfig
                rateLimiter(
                    limit = rateLimitConfig.rateLimit,
                    refillPeriod = rateLimitConfig.refillSpeed,
                    initialSize = rateLimitConfig.initialSize
                )
            }
            register(RateLimitName("imageUploading")) {
                rateLimiter(limit = 3, refillPeriod = 60.seconds, initialSize = 5)
            }
        }
}

fun Application.routing() {
    routing {
        logger.debug { "initializing routing" }
        miscRouting()
        monitoringRouting()
        route("/api/v1/user/") {
            userInfoRouting()
            gameRouting()
            accountRouting()
        }
    }
}

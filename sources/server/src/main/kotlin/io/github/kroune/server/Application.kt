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
package io.github.kroune.server

import appVersions.di.versionModule
import botsImpl.di.botsModules
import botsApi.dao.BotsServiceI
import common.ConfigurationLoader.currentConfig
import common.commonModule
import common.logging.logger
import database.di.databaseModule
import gameCommon.data.dao.GamesDataServiceI
import gameMain.di.gameMainModules
import gameQueue.data.queue.dao.QueueServiceI
import gameQueue.di.queueModules
import io.github.kroune.server.routing.accountRouting
import io.github.kroune.server.routing.gameRouting
import io.github.kroune.server.routing.misc.miscRouting
import io.github.kroune.server.routing.monitoring.monitoringRouting
import io.github.kroune.server.routing.userInfoRouting
import io.github.kroune.server.routing.versionRouting
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.protobuf.protobuf
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.basic
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadDeadlockMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import user.di.usersModules
import userApi.data.dao.UsersDataServiceI
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
            databaseModule,
            versionModule,
            gameMainModules,
            usersModules,
            commonModule,
            queueModules,
            botsModules,
        )
    }
}

fun Application.database() {
    logger.debug { "initializing users repository" }
    get<UsersDataServiceI>()
    logger.debug { "initializing games repository" }
    get<GamesDataServiceI>()
    logger.debug { "initializing botsImpl repository" }
    get<BotsServiceI>()
    logger.debug { "initializing queue repository" }
    get<QueueServiceI>()
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

@OptIn(ExperimentalSerializationApi::class)
fun Application.applyPlugins(includeRateLimitPlugin: Boolean = true) {
    install(ContentNegotiation) {
        removeIgnoredType<ByteArray>()
        removeIgnoredType<String>()
        json()
        protobuf()
    }
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowHost("github.io", listOf("http", "https"), listOf("kroune"))
        allowHost("kroune.tech", listOf("http", "https"), listOf("play", "dev"))
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
        route("/api/v1/") {
            versionRouting()
            userInfoRouting()
            gameRouting()
            accountRouting()
        }
    }
}

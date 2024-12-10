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
package com.example

import com.example.common.json
import com.example.data.local.botsRepository
import com.example.data.local.gamesRepository
import com.example.data.local.queueRepository
import com.example.data.local.usersRepository
import com.example.features.Config
import com.example.features.currentConfig
import com.example.features.logging.identifier
import com.example.features.logging.log
import com.example.features.logging.openTelemetryEndpoint
import com.example.features.logging.openTelemetryLogger
import com.example.routing.auth.accountRouting
import com.example.routing.game.gameRouting
import com.example.routing.misc.miscRouting
import com.example.routing.userInfo.userInfoRouting
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.instrumentation.ktor.v3_0.server.KtorServerTracing
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.export.RetryPolicy
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun main() {
    log("starting server", Severity.INFO)
    log("printing server config", Severity.INFO)
    log(json.encodeToString<Config>(currentConfig), Severity.INFO)
    currentConfig
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
            applyPlugins()
            routing()
            database()
        }
    ).start(wait = true)
}

fun database() {
    val isInK8s = System.getenv("IS_IN_K8S") == "1"
    val localhost = "127.0.0.1:5432"
    val podDomain = "postgres-service.default.svc.cluster.local"
    val url = if (isInK8s) podDomain else localhost
    Database.connect(
        "jdbc:postgresql://$url/postgres",
        driver = "org.postgresql.Driver",
        user = currentConfig.dbConfig.username,
        password = currentConfig.dbConfig.password
    )
    log("initializing users repository", Severity.DEBUG)
    usersRepository
    log("initializing games repository", Severity.DEBUG)
    gamesRepository
    log("initializing bots repository", Severity.DEBUG)
    botsRepository
    log("initializing queue repository", Severity.DEBUG)
    queueRepository
    log("applying configs", Severity.DEBUG)
}

fun Application.applyPlugins(includeRateLimitPlugin: Boolean = true) {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
        val webSocketConfig = currentConfig.webSocketConfig
        pingPeriod = webSocketConfig.pingPeriod
        timeout = webSocketConfig.timeout
    }
    val openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addSpanProcessor(
                    BatchSpanProcessor.builder(
                        OtlpGrpcSpanExporter.builder()
                            .setEndpoint(openTelemetryEndpoint)
                            .setCompression("gzip")
                            .setRetryPolicy(RetryPolicy.getDefault())
                            .build()
                    )
                        .setExporterTimeout(3.seconds.toJavaDuration())
                        .build()
                )
                .setResource(
                    Resource.builder()
                        .put(ServiceAttributes.SERVICE_NAME, "nine-mens-morris-server")
                        .put(ServiceAttributes.SERVICE_VERSION, identifier)
                        .build()
                )
                .build()
        )
        .setLoggerProvider(
            openTelemetryLogger
        )
        .build()
    install(KtorServerTracing) {
        setOpenTelemetry(openTelemetry)

        knownMethods(HttpMethod.DefaultMethods)
        capturedRequestHeaders(HttpHeaders.UserAgent)
        capturedResponseHeaders(HttpHeaders.ContentType)

        spanStatusExtractor {
            if (error != null) {
                spanStatusBuilder.setStatus(StatusCode.ERROR)
            }
        }

        spanKindExtractor {
            if (httpMethod == HttpMethod.Post) {
                SpanKind.PRODUCER
            } else {
                SpanKind.CLIENT
            }
        }

        attributeExtractor {
            onStart {
                attributes.put("start-time", Instant.now().toEpochMilli())
            }
            onEnd {
                attributes.put("end-time", Instant.now().toEpochMilli())
            }
        }
    }
    if (includeRateLimitPlugin)
        install(RateLimit) {
            global {
                val rateLimitConfig = currentConfig.rateLimitConfig
                rateLimiter(limit = rateLimitConfig.rateLimit, refillPeriod = rateLimitConfig.refillSpeed)
            }
            register(RateLimitName("imageUploading")) {
                rateLimiter(limit = 3, refillPeriod = 60.seconds)
            }
        }
}

fun Application.routing() {
    routing {
        log("initializing routing", Severity.DEBUG)
        miscRouting()
        route("/api/v1/user/") {
            userInfoRouting()
            gameRouting()
            accountRouting()
        }
    }
}

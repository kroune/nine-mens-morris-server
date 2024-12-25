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
package com.example.features.logging

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.sdk.common.export.RetryPolicy
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

val identifier = UUID.randomUUID().toString()

val openTelemetryEndpoint: String = run {
    val isInK8s = System.getenv("IS_IN_K8S") == "1"
    val localhost = "http://127.0.0.1:4317"
    // we need to specify port
    val podDomain = "http://my-release-signoz-otel-collector.default.svc.cluster.local:4317"
    val endpoint = if (isInK8s) podDomain else localhost
    endpoint
}

val openTelemetryLogger: SdkLoggerProvider = SdkLoggerProvider.builder()
    .addLogRecordProcessor(
        BatchLogRecordProcessor.builder(
            OtlpGrpcLogRecordExporter.builder()
                .setEndpoint(openTelemetryEndpoint)
                .setCompression("gzip")
                .setRetryPolicy(
                    RetryPolicy.getDefault()
                )
                .build()
        )
            .build()
    )
    .setResource(
        Resource.builder()
            .put(ServiceAttributes.SERVICE_NAME, "nine-mens-morris-server")
            .put(ServiceAttributes.SERVICE_VERSION, identifier)
            .build()
    )
    .build()

val loggerInstance: Logger = openTelemetryLogger
    .loggerBuilder("nine-mens-morris-server")
    .build()

fun log(
    text: String,
    severity: Severity,
    userId: Long? = null,
    throwable: Throwable? = null,
    bucket: Int? = null,
    gameId: Long? = null,
) {
    val sdf = SimpleDateFormat("hh:mm:ss dd/M/yyyy ")
    val currentDate = sdf.format(Date())
    println("$currentDate $text ${throwable?.stackTraceToString() ?: ""}")
    loggerInstance.logRecordBuilder()
        .setBody(text)
        .apply {
            if (userId != null) {
                this.setAttribute(AttributeKey.longKey("userId"), userId)
            }
        }
        .apply {
            if (throwable != null) {
                this.setAttribute(AttributeKey.stringKey("exception"), throwable.message ?: "empty message")
                this.setAttribute(AttributeKey.stringKey("stackTrace"), throwable.stackTraceToString())
            }
        }
        .apply {
            if (bucket != null) {
                this.setAttribute(AttributeKey.longKey("bucketId"), bucket.toLong())
            }
        }
        .apply {
            if (gameId != null) {
                this.setAttribute(AttributeKey.longKey("gameId"), gameId.toLong())
            }
        }
        .setTimestamp(Instant.now())
        .setSeverity(severity)
        .emit()
}

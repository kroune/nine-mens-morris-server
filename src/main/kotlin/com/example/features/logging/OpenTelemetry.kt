package com.example.features.logging

import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.sdk.common.export.RetryPolicy
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes

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
                    RetryPolicy.builder()
                        .build()
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

package com.example.features.logging

val openTelemetryEndpoint: String = run {
    val isInK8s = System.getenv("IS_IN_K8S") == "1"
    val localhost = "http://127.0.0.1:4317"
    val podDomain = "http://my-release-signoz-otel-collector.default.svc.cluster.local"
    val endpoint = if (isInK8s) podDomain else localhost
    endpoint
}
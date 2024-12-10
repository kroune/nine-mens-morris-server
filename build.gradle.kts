plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    application
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("com.example.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.dokkaHtml {
    suppressObviousFunctions = true
}

dependencies {
    // ktor
    implementation(libs.ktor.server.ohhttp)
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.content.negotiation.jvm)
    implementation(libs.ktor.serialization.kotlinx.json.jvm)
    implementation(libs.ktor.server.netty.jvm)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.client.okhttp)

    // other libs
    implementation(libs.logback.classic)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(kotlin("reflect"))
    implementation(libs.bcrypt)

    // serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)

    // db
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.json)
    implementation(libs.postgresql)

    // my own dependencies
    implementation(libs.nine.men.s.morris.shared)
    implementation(libs.nine.men.s.morris.lib)

    // otlp
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.opentelemetry.ktor)

    // testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.test.container.postgresql)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test)
}

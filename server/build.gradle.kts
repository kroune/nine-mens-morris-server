plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    application
}

group = "io.github.kroune"
version = "0.0.1"

application {
    mainClass.set("io.github.kroune.server.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.dokkaHtml {
    suppressObviousFunctions = true
}

dependencies {
    implementation(project(":database"))

    // ktor
    implementation(libs.ktor.server.ohhttp)
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.content.negotiation.jvm)
    implementation(libs.ktor.server.netty.jvm)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.cors)

    implementation(libs.ktor.client.okhttp)

    // logging
    implementation(libs.logback.classic)
    implementation(libs.logback.loki)

    // my own dependencies
    implementation(libs.nine.men.s.morris.shared)

    // micrometer
    implementation(libs.ktor.micrometer)
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.3")

    api(project(":common"))
    implementation(project(":game-queue"))
    implementation(project(":user"))

    // testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(kotlin("reflect"))
}

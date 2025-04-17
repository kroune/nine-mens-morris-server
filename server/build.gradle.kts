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
    mainClass.set("io.github.kroune.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.dokkaHtml {
    suppressObviousFunctions = true
}

dependencies {
    // db
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.json)
    implementation(libs.postgresql)

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

    // other libs
    implementation(libs.bcrypt)

    // logging
    implementation(libs.logback.classic)
    implementation(libs.logback.loki)

    // serialization
    implementation(libs.ktor.serialization.kotlinx.json.jvm)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kaml)

    // my own dependencies
    implementation(libs.nine.men.s.morris.shared)

    // micrometer
    implementation("io.ktor:ktor-server-metrics-micrometer:3.0.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.3")

    // datetime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    // koin
    implementation(libs.insert.koin.core)
    implementation(libs.insert.koin.ktor3)

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

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "io.github.kroune"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    // logging
    api(libs.logback.classic)
    api(libs.logback.loki)
    api("io.github.oshai:kotlin-logging-jvm:7.0.3")

    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.core.jvm)

    // koin
    api(libs.insert.koin.core)
    api(libs.insert.koin.ktor3)

    // datetime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    // serialization
    api(libs.ktor.serialization.kotlinx.json.jvm)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.serialization.protobuf)
    api(libs.kaml)

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)

    api(libs.ktor.server.rate.limit)


    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
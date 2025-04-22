plugins {
    kotlin("jvm") version "2.1.0"
}

group = "io.github.kroune"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    // db
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.json)
    implementation(libs.postgresql)

    api(libs.test.container.kafka)
    api(libs.test.container.postgresql)
    api(project(":common"))
    testImplementation(kotlin("test"))
    api(libs.ktor.server.test)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
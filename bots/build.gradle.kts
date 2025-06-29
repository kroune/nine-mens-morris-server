plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.jvm)
}

group = "io.github.kroune"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":database"))

    // kafka
    implementation("org.apache.kafka:kafka-clients:4.0.0")

    implementation(libs.ktor.server.websockets)

    testImplementation(libs.test.container.kafka)

    implementation(libs.ktor.client.okhttp)

    implementation(libs.nine.men.s.morris.lib)

    api(project(":common"))
    api(project(":user"))

    testImplementation(libs.test.container.postgresql)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}
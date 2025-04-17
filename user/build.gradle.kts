plugins {
    alias(libs.plugins.kotlin.jvm)
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

    implementation(libs.ktor.server.websockets)

    // other libs
    implementation(libs.bcrypt)

    implementation(project(":common"))

    implementation(libs.ktor.server.core.jvm)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
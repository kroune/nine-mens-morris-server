plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "io.github.kroune"
version = "unspecified"

dependencies {
    // db
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.json)
    implementation(libs.postgresql)

    // kafka
    implementation("org.apache.kafka:kafka-clients:4.0.0")

    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.websockets)

    testImplementation(libs.test.container.kafka)

    implementation(libs.nine.men.s.morris.lib)

    api(project(":common"))
    api(project(":user"))
    api(project(":bots"))

    testApi(project(":common-tests"))
    testApi(project(":user-tests"))

    // my own dependencies
    implementation(libs.nine.men.s.morris.shared)

    testImplementation(libs.test.container.postgresql)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":database"))

    api(libs.test.container.kafka)
    api(libs.test.container.postgresql)
    api(project(":common"))
    testImplementation(kotlin("test"))
    api(libs.ktor.server.test)
}

tasks.test {
    useJUnitPlatform()
}
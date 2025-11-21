plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {

    // kafka
    implementation("org.apache.kafka:kafka-clients:4.0.0")

    implementation(libs.ktor.server.websockets)

    testImplementation(libs.test.container.kafka)

    implementation(libs.nine.men.s.morris.lib)

    api(project(":database"))
    api(project(":common"))
//    api(project(":user"))
    implementation(project(":bots-api"))

    testApi(project(":common-tests"))
    testApi(project(":user-tests"))

    // my own dependencies
    implementation(libs.nine.men.s.morris.shared)

    testImplementation(libs.test.container.postgresql)
    testImplementation(kotlin("test"))
}
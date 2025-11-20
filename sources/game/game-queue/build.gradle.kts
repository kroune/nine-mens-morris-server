plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // kafka
    implementation("org.apache.kafka:kafka-clients:4.0.0")

    implementation(libs.ktor.server.websockets)

    implementation(libs.nine.men.s.morris.lib)

    api(project(":database"))
    api(project(":common"))
    implementation(project(":user-api"))
    implementation(project(":bots-api"))
    api(project(":game-common"))

    testApi(project(":common-tests"))
    testApi(project(":user-tests"))

    testImplementation(libs.test.container.postgresql)
    testImplementation(kotlin("test"))
    testImplementation(project(":game-main"))
    testImplementation(project(":user"))
    testImplementation(project(":bots"))
}

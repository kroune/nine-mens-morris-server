plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":database"))
    // db
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    api(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.json)
    implementation(libs.postgresql)

    implementation(libs.ktor.server.websockets)

    // other libs
    implementation(libs.bcrypt)

    api(project(":common"))

    implementation(libs.ktor.server.core.jvm)

    testImplementation(project(":common-tests"))
    testImplementation(project(":user-tests"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
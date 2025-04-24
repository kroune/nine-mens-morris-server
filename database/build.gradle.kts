plugins {
    kotlin("jvm") version "2.1.0"
}

dependencies {
    // db
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    api(libs.exposed.json)
    implementation(libs.postgresql)
    testImplementation(kotlin("test"))

    api(project(":common"))
}

tasks.test {
    useJUnitPlatform()
}
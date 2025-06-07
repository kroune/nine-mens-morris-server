plugins {
    alias(libs.plugins.kotlin.jvm)
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
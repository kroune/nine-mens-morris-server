plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":common-tests"))
    api(project(":common"))
    api(project(":user-api"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
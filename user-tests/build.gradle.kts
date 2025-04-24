plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":user"))
    api(project(":common-tests"))
    api(project(":common"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":common"))
    api(project(":database"))
    testImplementation(project(":common-tests"))
    testImplementation(project(":user-tests"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
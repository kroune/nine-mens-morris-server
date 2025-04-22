plugins {
    kotlin("jvm") version "2.1.0"
}

group = "io.github.kroune"
version = "unspecified"

repositories {
    mavenCentral()
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
kotlin {
    jvmToolchain(21)
}
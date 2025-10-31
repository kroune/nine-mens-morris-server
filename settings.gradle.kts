plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "NineMensMorris-backend"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
            name = "Ktor EAP"
        }
        maven {
            url = uri("https://jitpack.io")
            name = "JitPack"
        }
        maven { url = uri("https://packages.confluent.io/maven/") }
        google()
    }
}

include(":common")
include("server")
include("database")
//include("common-ktor")
include("game-queue")
include("user")
//include("game-server")
include("game-main")
include("bots")
include("common-tests")
include("user-tests")
include("database")

include("app-versions")
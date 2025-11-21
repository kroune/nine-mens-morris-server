package appVersion

import appVersions.di.versionModule
import common.commonModule
import commonTests.di.commonTestModule
import database.di.databaseModule
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.protobuf.protobuf
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.ktor.plugin.Koin

@OptIn(ExperimentalSerializationApi::class)
fun Application.applyPlugins() {
    install(ContentNegotiation) {
        json()
        protobuf()
    }
}

fun Application.startTestDI() {
    install(Koin) {
        modules(
            koinModules
        )
    }
}

val koinModules = listOf(
    databaseModule,
    versionModule,
    commonModule,
    commonTestModule,
)
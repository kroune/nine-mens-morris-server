package appVersion

import common.commonModule
import commonTests.di.commonTestModule
import database.di.databaseModule
import appVersions.di.versionModule
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.protobuf.protobuf
import io.ktor.server.application.*
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
package user

import common.commonModule
import commonTests.di.commonTestModule
import database.di.databaseModule
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.plugin.Koin
import user.di.usersModules

fun Application.startTestDI() {
    install(Koin) {
        modules(
            koinModules
        )
    }
}

val koinModules = listOf(
    commonModule,
    commonTestModule,
    databaseModule,
    usersModules,
)
import common.commonModule
import commonTests.di.commonTestModule
import database.di.databaseModule
import di.versionModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

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
//    databaseModule,
//    usersModules,
)
package common

import ch.qos.logback.classic.Logger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.dsl.module

val commonModule = module {
    single { ConfigurationLoader.currentConfig }
    single { KotlinLogging.logger(Logger.ROOT_LOGGER_NAME) }
}
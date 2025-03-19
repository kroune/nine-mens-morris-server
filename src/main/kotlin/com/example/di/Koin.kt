package com.example.di

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.dsl.module

val koinModules = module {
    single { KotlinLogging.logger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) }
}
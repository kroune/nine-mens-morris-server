package io.github.kroune.di

import io.github.kroune.data.local.users.dao.UsersDataServiceI
import io.github.kroune.data.local.users.dao.UsersDataServiceImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.dsl.module

val koinModules = module {
    single { KotlinLogging.logger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) }
    single<UsersDataServiceI> { UsersDataServiceImpl() }
}
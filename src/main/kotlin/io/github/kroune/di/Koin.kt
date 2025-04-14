package io.github.kroune.di

import io.github.kroune.data.local.users.dao.UsersDataServiceI
import io.github.kroune.data.local.users.dao.UsersDataServiceImpl
import io.github.kroune.features.ConfigurationLoader.ConfigMember
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.dsl.module
import java.util.*

val koinModules = module {
    single { KotlinLogging.logger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) }
    single<UsersDataServiceI> { UsersDataServiceImpl() }
    factory {
        val config = get<ConfigMember>()
        val props = Properties()
        props.put("bootstrap.servers", config.kafkaConfig.url)
        props
    }
}
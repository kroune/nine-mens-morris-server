package io.github.kroune.di

import data.dao.UsersDataServiceI
import data.dao.UsersDataServiceImpl
import ConfigurationLoader.ConfigMember
import bots.dao.BotsServiceI
import bots.dao.BotsServiceImpl
import data.dao.GamesDataServiceI
import data.dao.GamesDataServiceImpl
import io.github.kroune.controller.GameController
import io.github.kroune.controller.UsersController
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.dsl.module
import queue.dao.QueueServiceI
import queue.dao.QueueServiceImpl
import randomUser.RandomUserRepositoryI
import randomUser.RandomUserRepositoryImpl
import java.util.*

val koinModules = module {
    single { KotlinLogging.logger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME) }
    single<UsersDataServiceI> { UsersDataServiceImpl() }
    single<QueueServiceI> { QueueServiceImpl() }
    single<GamesDataServiceI> { GamesDataServiceImpl() }
    single<BotsServiceI> { BotsServiceImpl() }
    single<UsersController> { UsersController() }
    single<GameController> { GameController() }
    single<RandomUserRepositoryI> { RandomUserRepositoryImpl() }
    factory {
        val config = get<ConfigMember>()
        val props = Properties()
        props.put("bootstrap.servers", config.kafkaConfig.url)
        props
    }
}
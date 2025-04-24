package gameQueue.di

import bots.dao.BotsServiceI
import bots.dao.BotsServiceImpl
import common.ConfigurationLoader.ConfigMember
import database.DatabaseConfiguration
import gameQueue.controller.QueueController
import gameQueue.data.queue.dao.QueueServiceI
import gameQueue.data.queue.dao.QueueServiceImpl
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import randomUser.RandomUserRepositoryI
import randomUser.RandomUserRepositoryImpl
import java.util.*

val queueModules = module {
    single<BotsServiceI> {
        BotsServiceImpl(
            get<Database>(named(DatabaseConfiguration.BOTS_DATA))
        )
    }
    single<RandomUserRepositoryI> { RandomUserRepositoryImpl() }
    single<QueueServiceI> { QueueServiceImpl() }
    single { QueueController(get()) }
    factory {
        val config = get<ConfigMember>()
        val props = Properties()
        props.put("bootstrap.servers", config.kafkaConfig.url)
        props
    }
}
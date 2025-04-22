package gameQueue.di

import common.ConfigurationLoader.ConfigMember
import bots.dao.BotsServiceI
import bots.dao.BotsServiceImpl
import gameQueue.controller.QueueController
import org.koin.dsl.module
import gameQueue.data.queue.dao.QueueServiceI
import gameQueue.data.queue.dao.QueueServiceImpl
import randomUser.RandomUserRepositoryI
import randomUser.RandomUserRepositoryImpl
import java.util.Properties

val queueModules = module {
    single<BotsServiceI> { BotsServiceImpl() }
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
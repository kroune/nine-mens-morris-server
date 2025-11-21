package gameQueue.di

import common.ConfigurationLoader.ConfigMember
import gameQueue.data.queue.dao.QueueServiceI
import gameQueue.data.queue.dao.QueueServiceImpl
import gameQueue.service.QueueService
import org.koin.dsl.module
import java.util.*

val queueModules = module {
    single<QueueServiceI> { QueueServiceImpl() }
    single {
        QueueService(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    factory {
        val config = get<ConfigMember>()
        val props = Properties()
        props["bootstrap.servers"] = config.kafkaConfig.url
        props
    }
}
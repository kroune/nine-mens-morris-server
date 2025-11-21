package bots.di

import bots.BotCreator
import bots.BotProvider
import botsApi.dao.BotsServiceI
import botsApi.randomUser.RandomUserRepositoryI
import bots.dao.BotsServiceImpl
import database.DatabaseConfiguration
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import bots.randomUser.RandomUserRepositoryImpl
import botsApi.BotCreatorI
import botsApi.BotProviderI

val botsModules = module {
    single<BotsServiceI> {
        BotsServiceImpl(
            get<Database>(named(DatabaseConfiguration.BOTS_DATA))
        )
    }
    single<RandomUserRepositoryI> { RandomUserRepositoryImpl() }
    single<BotProviderI> { BotProvider() }
    single<BotCreatorI> { BotCreator() }
}

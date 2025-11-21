package botsImpl.di

import botsImpl.BotCreator
import botsImpl.BotProvider
import botsImpl.dao.BotsServiceImpl
import botsImpl.randomUser.RandomUserRepositoryImpl
import botsApi.BotCreatorI
import botsApi.BotProviderI
import botsApi.dao.BotsServiceI
import botsApi.randomUser.RandomUserRepositoryI
import database.DatabaseConfiguration
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module

val botsModules = module {
    single<BotsServiceI> {
        BotsServiceImpl(
            get<Database>(named(DatabaseConfiguration.BOTS_DATA))
        )
    }
    single<RandomUserRepositoryI> { RandomUserRepositoryImpl() }
    single<BotProviderI> {
        BotProvider(
            get(),
            get(),
            get()
        )
    }
    single<BotCreatorI> {
        BotCreator(
            get(),
            get(),
            get(),
            get()
        )
    }
}

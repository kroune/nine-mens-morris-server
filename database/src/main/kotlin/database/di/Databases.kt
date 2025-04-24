package database.di

import common.ConfigurationLoader
import database.DatabaseConfiguration
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module

val databaseModule = module {
    single(named(DatabaseConfiguration.USER_DATA)) {
        val config = get<ConfigurationLoader.ConfigMember>()
        val userDataConfig = config.databasesConfig.userData
        Database.connect(
            url = userDataConfig.url,
            driver = "org.postgresql.Driver",
            user = userDataConfig.username,
            password = userDataConfig.password
        )
    }
    single(named(DatabaseConfiguration.BOTS_DATA)) {
        val config = get<ConfigurationLoader.ConfigMember>()
        val userDataConfig = config.databasesConfig.botsData
        Database.connect(
            url = userDataConfig.url,
            driver = "org.postgresql.Driver",
            user = userDataConfig.username,
            password = userDataConfig.password
        )
    }
}
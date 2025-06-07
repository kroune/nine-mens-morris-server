package user.di

import database.DatabaseConfiguration
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import user.controller.UsersController
import user.data.dao.UsersDataServiceI
import user.data.dao.UsersDataServiceImpl

val usersModules = module {
    single<UsersDataServiceI> {
        UsersDataServiceImpl(get<Database>(named(DatabaseConfiguration.USER_DATA)))
    }
    single<UsersController> { UsersController(get()) }
}
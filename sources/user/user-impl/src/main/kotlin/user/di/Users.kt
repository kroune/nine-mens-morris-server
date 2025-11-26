package user.di

import database.DatabaseConfiguration
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import user.data.dao.UsersDataServiceImpl
import user.domain.UsersService
import userApi.data.dao.UsersDataServiceI
import userApi.domain.UsersServiceI

val usersModules = module {
    single<UsersDataServiceI> {
        UsersDataServiceImpl(get<Database>(named(DatabaseConfiguration.USER_DATA)))
    }
    single<UsersServiceI> { UsersService(get()) }
}
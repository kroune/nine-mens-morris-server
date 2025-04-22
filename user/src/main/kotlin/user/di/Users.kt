package user.di

import user.data.dao.UsersDataServiceI
import user.data.dao.UsersDataServiceImpl
import user.controller.UsersController
import org.koin.dsl.module

val usersModules = module {
    single<UsersDataServiceI> { UsersDataServiceImpl() }
    single<UsersController> { UsersController() }
}
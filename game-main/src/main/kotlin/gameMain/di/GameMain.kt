package gameMain.di

import gameMain.data.dao.GamesDataServiceI
import gameMain.data.dao.GamesDataServiceImpl
import org.koin.dsl.module

val gameMainModules = module {
    single<GamesDataServiceI> { GamesDataServiceImpl() }
}
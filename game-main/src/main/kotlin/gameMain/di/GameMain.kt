package gameMain.di

import gameMain.controller.GameController
import gameMain.data.dao.GamesDataServiceI
import gameMain.data.dao.GamesDataServiceImpl
import org.koin.dsl.module

val gameMainModules = module {
    single<GameController> { GameController() }
    single<GamesDataServiceI> { GamesDataServiceImpl() }
}
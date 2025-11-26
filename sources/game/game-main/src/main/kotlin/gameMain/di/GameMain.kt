package gameMain.di

import gameCommon.GameI
import gameCommon.data.dao.GamesDataServiceI
import gameMain.Game
import gameMain.data.dao.GamesDataServiceImpl
import io.ktor.server.websocket.DefaultWebSocketServerSession
import org.koin.dsl.module

val gameMainModules = module {
    single<GamesDataServiceI> { GamesDataServiceImpl() }
    factory<GameI> { (gameId: Long,
                         firstPlayer: DefaultWebSocketServerSession?,
                         secondPlayer: DefaultWebSocketServerSession?) ->
        Game(
            gameId,
            firstPlayer,
            secondPlayer,
            get(),
            get(),
            get(),
        )
    }
}
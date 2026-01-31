package gameMain.di

import gameCommon.GameI
import gameCommon.data.dao.GamesDataServiceI
import gameMain.Game
import gameMain.data.dao.gameData.GamesDataServiceImpl
import gameMain.data.dao.movesHistory.MovesHistoryServiceI
import gameMain.data.dao.movesHistory.MovesHistoryServiceImpl
import io.ktor.server.websocket.DefaultWebSocketServerSession
import org.koin.dsl.module

val gameMainModules = module {
    single<GamesDataServiceI> { GamesDataServiceImpl() }
    single<MovesHistoryServiceI> { MovesHistoryServiceImpl() }
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
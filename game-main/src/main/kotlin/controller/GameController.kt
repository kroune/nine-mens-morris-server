package io.github.kroune.controller

import data.dao.GameData
import data.dao.GamesDataServiceI
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GameController(): KoinComponent {
    val gamesDataService by inject<GamesDataServiceI>()

    suspend fun create(game: GameData): Boolean {
        return gamesDataService.create(game)
    }

    suspend fun participates(userId: Long): Boolean {
        return gamesDataService.participates(userId)
    }
}
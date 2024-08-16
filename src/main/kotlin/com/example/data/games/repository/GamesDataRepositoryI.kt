package com.example.data.games.repository

import com.example.data.games.GameData
import com.kroune.nineMensMorrisLib.Position
import com.kroune.nineMensMorrisLib.move.Movement

interface GamesDataRepositoryI {
    suspend fun create(game: GameData)
    suspend fun getPositionByGameId(gameId: Long): Position?
    suspend fun getGameMoveHistory(gameId: Long): List<Movement>?
    suspend fun applyMove(gameId: Long, move: Movement)
    suspend fun getFirstUserIdByGameId(gameId: Long): Long?
    suspend fun getSecondUserIdByGameId(gameId: Long): Long?
    suspend fun getFirstPlayerMovesFirstByGameId(gameId: Long): Boolean?
    suspend fun getBotIdByGameId(gameId: Long): Long?
    suspend fun getMovesCountByGameId(gameId: Long): Int?
    suspend fun getGameIdByUserId(userId: Long): Long?
    suspend fun participates(userId: Long): Boolean
    suspend fun exists(gameId: Long): Boolean
}
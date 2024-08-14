package com.example.game

import java.util.*
import java.util.concurrent.atomic.AtomicLong

object GamesDB {
    private val loginToGameIdMap: MutableMap<String, Long> = Collections.synchronizedMap(mutableMapOf())
    private val games: MutableMap<Long, GameData> = Collections.synchronizedMap(mutableMapOf<Long, GameData>())
    private val atomicGameId = AtomicLong(0)

    fun getGame(id: Long): GameData? {
        return games[id]
    }

    fun createGame(firstUser: Connection, secondUser: Connection, botAtGame: Boolean): Long {
        val id = atomicGameId.incrementAndGet()
        games[id] = GameData(firstUser, secondUser, id, botAtGame)
        loginToGameIdMap[firstUser.login] = id
        loginToGameIdMap[secondUser.login] = id
        println("created game with id - $id and users - ${firstUser.login} and ${secondUser.login}")
        return id
    }


    fun gameId(login: String): Result<Long?> {
        return runCatching {
            loginToGameIdMap[login]
        }
    }
}

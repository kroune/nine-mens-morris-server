package com.example.game

import java.util.*
import java.util.concurrent.atomic.AtomicLong

object Games {
    val map: MutableMap<String, Long> = mutableMapOf()
    fun getGame(id: Long): GameData? {
        return games[id]
    }

    fun createGame(firstUser: Connection, secondUser: Connection): Long {
        val id = atomicGameId.incrementAndGet()
        games[id] = GameData(firstUser, secondUser)
        map[firstUser.jwtToken] = id
        map[secondUser.jwtToken] = id
        return id
    }

    val games: MutableMap<Long, GameData> = Collections.synchronizedMap(mutableMapOf<Long, GameData>())
    val atomicGameId = AtomicLong(0)

    fun gameId(jwtToken: String): Long? {
        return map[jwtToken]
    }
}
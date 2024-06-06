package com.example.game

import com.example.jwtToken.CustomJwtToken
import java.util.*
import java.util.concurrent.atomic.AtomicLong

object Games {
    private val gamesMap: MutableMap<String, Long> = Collections.synchronizedMap(mutableMapOf())
    private val games: MutableMap<Long, GameData> = Collections.synchronizedMap(mutableMapOf<Long, GameData>())
    private val atomicGameId = AtomicLong(0)

    fun getGame(id: Long): GameData? {
        return games[id]
    }

    fun createGame(firstUser: Connection, secondUser: Connection): Long {
        synchronized(this) {
            val id = atomicGameId.incrementAndGet()
            games[id] = GameData(firstUser, secondUser)
            gamesMap[firstUser.jwtToken.getLogin().getOrThrow()] = id
            gamesMap[secondUser.jwtToken.getLogin().getOrThrow()] = id
            println("created game with ${firstUser.jwtToken.token} and ${secondUser.jwtToken.token}")
            return id
        }
    }


    fun gameId(jwtToken: CustomJwtToken): Long? {
        return gamesMap[jwtToken.getLogin().getOrThrow()]
    }
}
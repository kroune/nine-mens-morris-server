package com.example.game.searching

import com.example.game.Connection
import com.example.game.Games
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

object SearchingForGame {
    private val usersSearchingForGameMap: MutableMap<String, Boolean> = mutableMapOf()
    private val usersSearchingForGame: Queue<Connection> = LinkedList()

    suspend fun addUser(user: Connection) {
        println("starting to add user")
        var gameId: Long?
        synchronized(this) {
            if (usersSearchingForGameMap[user.jwtToken.getLogin().getOrThrow()] == true) {
                // user is already searching for game
                println("already searching")
                return
            }
            gameId = Games.gameId(user.jwtToken).getOrNull()
            if (gameId != null) {
                return@synchronized
            }
            println("added user to the queue ${user.jwtToken.getLogin().getOrNull()}")
            usersSearchingForGame.add(user)
            usersSearchingForGameMap[user.jwtToken.getLogin().getOrThrow()] = true
        }
        if (gameId != null) {
            println("already playing")
            // user is already in game
            user.session.send(gameId.toString())
            user.session.close()
            return
        }
    }

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                if (usersSearchingForGame.size < 2) {
                    delay(2000)
                    println("skipped")
                    continue
                }
                val firstUser = usersSearchingForGame.poll()!!
                val secondUser = usersSearchingForGame.poll()!!
                val id = Games.createGame(firstUser, secondUser)
                usersSearchingForGameMap.remove(firstUser.jwtToken.getLogin().getOrThrow())
                usersSearchingForGameMap.remove(secondUser.jwtToken.getLogin().getOrThrow())
                firstUser.session.send(id.toString())
                secondUser.session.send(id.toString())
                firstUser.session.close()
                secondUser.session.close()
            }
        }
    }
}
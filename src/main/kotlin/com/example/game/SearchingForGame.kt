package com.example.game

import com.example.CustomJwtToken
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.util.*

object SearchingForGame {
    private val usersSearchingForGameMap: MutableMap<String, Boolean> = mutableMapOf()
    private val usersSearchingForGame: Queue<Connection> = LinkedList()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun addUser(user: Connection) {
        assert(user.session != null)
        println("new user")
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
            CoroutineScope(Dispatchers.IO.limitedParallelism(100)).launch {
                delay(20_000)
                // check if we are still searching
                if (usersSearchingForGameMap[user.jwtToken.getLogin().getOrThrow()] == true) {
                    println("no enemy was found for the user ${user.id().getOrNull()}, pairing with bot")
                    // we can't use any const value, or it would be possible to send moves from bot side
                    val botJwtToken = CustomJwtToken(login = getRandomString(8), password = getRandomString(8))
                    val secondUser = Connection(botJwtToken, null)
                    val id = Games.createGame(user, secondUser)
                    usersSearchingForGameMap.remove(user.jwtToken.getLogin().getOrThrow())
                    user.session?.send(id.toString())
                    user.session?.close()
                }
            }
        }
        if (gameId != null) {
            println("already playing")
            // user is already in game
            user.session?.send(gameId.toString())
            user.session?.close()
            return
        }
    }

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                if (usersSearchingForGame.size < 2) {
                    delay(2000)
                    continue
                }
                val firstUser = usersSearchingForGame.poll()!!
                val secondUser = usersSearchingForGame.poll()!!
                val id = Games.createGame(firstUser, secondUser)
                usersSearchingForGameMap.remove(firstUser.jwtToken.getLogin().getOrThrow())
                usersSearchingForGameMap.remove(secondUser.jwtToken.getLogin().getOrThrow())
                firstUser.session!!.send(id.toString())
                secondUser.session!!.send(id.toString())
                firstUser.session!!.close()
                secondUser.session!!.close()
            }
        }
    }
}

fun getRandomString(length: Int) : String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

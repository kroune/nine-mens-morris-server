package com.example.game

import com.example.CustomJwtToken
import com.example.LogPriority
import com.example.log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*

object SearchingForGame {
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchingForGameScope = Dispatchers.IO.limitedParallelism(100)
    private val usersSearchingForGameJobsMap: MutableMap<String, Job> = mutableMapOf()

    val bucketSize = 100
    val delayBeforeRecheckingBucket = 1_000L

    /**
     * array of buckets, represented by queue from [Pair] of [Connection] and [Channel]
     */
    private val usersSearchingForGame: Array<Queue<Pair<Connection, Channel<Pair<Boolean, Long>>>>> = Array(50) {
        LinkedList()
    }

    suspend fun addUser(user: Connection, channel: Channel<Pair<Boolean, Long>>) {
        val login = user.jwtToken.getLogin().getOrThrow()
        val oldJob = usersSearchingForGameJobsMap[login]
        // cancel previous searching if it exist
        oldJob?.cancel()
        val job = CoroutineScope(searchingForGameScope).launch {
            require(user.session != null)
            val gameId: Long? = Games.gameId(user.jwtToken).getOrNull()
            if (gameId != null) {
                // user is already in a game
                channel.send(Pair(false, gameId))
                usersSearchingForGameJobsMap.remove(login)
                return@launch
            }
            log("Added user to the queue $login", LogPriority.Debug)
            val queueToAddUser = (user.raiting().getOrThrow() / bucketSize).toInt()
            usersSearchingForGame[queueToAddUser].add(Pair(user, channel))
            delay(20_000)
            // check if we are still searching
            if (usersSearchingForGameJobsMap[login] != null) {
                println("no enemy was found for the user ${user.id().getOrNull()}, pairing with bot")
                // we can't use any const value, or it would be possible to send moves from bot side
                val botJwtToken = CustomJwtToken(login = getRandomString(8), password = getRandomString(8))
                val secondUser = Connection(botJwtToken, null)
                val id = Games.createGame(user, secondUser)
                channel.send(Pair(false, id))
                usersSearchingForGameJobsMap.remove(login)
            }
        }
        usersSearchingForGameJobsMap[login] = job
    }

    init {
        usersSearchingForGame.forEach { bucket ->
            CoroutineScope(searchingForGameScope).launch {
                while (true) {
                    if (bucket.size < 2) {
                        // TODO: add average game search time updater
                        val expectedWaitingTime = 15L
                        bucket.peek()?.second?.trySend(Pair(true, expectedWaitingTime))
                        delay(delayBeforeRecheckingBucket)
                        continue
                    }
                    val firstUser = bucket.poll()!!
                    val secondUser = bucket.poll()!!
                    val id = Games.createGame(firstUser.first, secondUser.first)
                    listOf(firstUser, secondUser).forEach { (connection, channel) ->
                        val login = connection.jwtToken.getLogin().getOrThrow()
                        channel.send(Pair(false, id))
                        usersSearchingForGameJobsMap.remove(login)
                    }
                }
            }
        }
    }
}

fun getRandomString(length: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length).map { allowedChars.random() }.joinToString("")
}

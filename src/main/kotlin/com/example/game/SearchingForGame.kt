package com.example.game

import com.example.LogPriority
import com.example.currentConfig
import com.example.data.usersRepository
import com.example.log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

val bucketSize = currentConfig.gameConfig.bucketSize
val delayBeforeRecheckingBucket = currentConfig.gameConfig.delayBeforeRecheckingBucket

object SearchingForGame {
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchingForGameScope = Dispatchers.IO.limitedParallelism(100)
    private val usersSearchingForGameJobsMap: MutableMap<String, Job> = mutableMapOf()

    /**
     * array of buckets, represented by queue from [Pair] of [Connection] and [Channel]
     */
    private val usersSearchingForGame: Array<Queue<Pair<Connection, Channel<Pair<Boolean, Long>>>>> = Array(50) {
        ConcurrentLinkedQueue()
    }

    suspend fun removeUser(user: Connection) {
        val login = user.login
        val queueToAddUser = (user.rating() / bucketSize).toInt()
        usersSearchingForGameJobsMap[login]?.cancel()
        // TODO: optimize this
        usersSearchingForGame[queueToAddUser].removeAll {
            it.first == user
        }
    }

    suspend fun addUser(user: Connection, channel: Channel<Pair<Boolean, Long>>) {
        val login = user.login
        val oldJob = usersSearchingForGameJobsMap[login]
        // cancel previous searching if it exists
        oldJob?.cancel()
        val job = CoroutineScope(searchingForGameScope).launch {
            require(user.session != null)
            val gameId: Long? = GamesDB.gameId(login).getOrNull()
            if (gameId != null) {
                // user is already in a game
                channel.send(Pair(false, gameId))
                usersSearchingForGameJobsMap.remove(login)
                return@launch
            }
            log("Added user to the queue $login", LogPriority.Debug)
            val queueToAddUser = (user.rating() / bucketSize)
            usersSearchingForGame[queueToAddUser].add(Pair(user, channel))
            delay(20_000)
            // check if we are still searching
            if (usersSearchingForGameJobsMap[login] != null) {
                println("no enemy was found for the user ${user.id()}, pairing with bot")
                // we can't use any const value, or it would be possible to send moves from bot side
                val botId = BotGenerator.getBotFromBucket(queueToAddUser)
                val botLogin = usersRepository.getLoginById(botId)!!
                val bot = Connection(botLogin, null)
                val id = GamesDB.createGame(user, bot, true)
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
                    if (bucket.size > 0) {
                        log("bucket.size - ${bucket.size}", LogPriority.Debug)
                    }
                    if (bucket.size < 2) {
                        // TODO: add average game search time updater
                        val expectedWaitingTime = 15L
                        bucket.peek()?.second?.trySend(Pair(true, expectedWaitingTime))
                        delay(delayBeforeRecheckingBucket)
                        continue
                    }
                    val firstUser = bucket.poll()!!
                    val secondUser = bucket.poll()!!
                    val id = GamesDB.createGame(firstUser.first, secondUser.first, false)
                    listOf(firstUser, secondUser).forEach { (connection, channel) ->
                        val login = connection.login
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

package com.example.game

import com.example.LogPriority
import com.example.currentConfig
import com.example.data.games.GameData
import com.example.data.gamesRepository
import com.example.data.usersRepository
import com.example.log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

val minPairWithBotTime = currentConfig.gameConfig.minTimeBeforePairingWithBot
val maxPairWithBotTime = currentConfig.gameConfig.maxTimeBeforePairingWithBot
val bucketSize = currentConfig.gameConfig.bucketSize
val delayBeforeRecheckingBucket = currentConfig.gameConfig.delayBeforeRecheckingBucket

object SearchingForGame {
    val searchingForGameScope = Dispatchers.IO
    private val usersSearchingForGameJobsMap: MutableMap<Long, Job> = mutableMapOf()

    /**
     * array of buckets, represented by queue from [Pair] of [Connection] and [Channel]
     */
    private val bucketsOfUsersSearchingForGame: Array<Queue<Pair<Long, Channel<Pair<Boolean, Long>>>>> = Array(50) {
        ConcurrentLinkedQueue()
    }

    suspend fun removeUser(userId: Long) {
        val rating = usersRepository.getRatingById(userId)!!
        val queueToAddUser = (rating / bucketSize).toInt()
        usersSearchingForGameJobsMap[userId]?.cancel()
        // TODO: optimize this
        bucketsOfUsersSearchingForGame[queueToAddUser].removeAll {
            it.first == userId
        }
    }
    suspend fun addUser(userId: Long, channel: Channel<Pair<Boolean, Long>>) {
        val rating = usersRepository.getRatingById(userId)!!
        val oldJob = usersSearchingForGameJobsMap[userId]
        // cancel previous searching if it exists
        oldJob?.cancel()
        val job = CoroutineScope(searchingForGameScope).launch {
            // coroutine should end normal, this is just to make sure
            withTimeout(24*60*60*1000) {
                val gameId: Long? = gamesRepository.getGameIdByUserId(userId)
                if (gameId != null) {
                    // user is already in a game
                    channel.send(Pair(false, gameId))
                    usersSearchingForGameJobsMap.remove(userId)
                    return@withTimeout
                }
                log("Added user to the queue $userId", LogPriority.Debug)
                val queueToAddUser = (rating / bucketSize)
                bucketsOfUsersSearchingForGame[queueToAddUser].add(Pair(userId, channel))
                val currentDelay = Random.nextLong(minPairWithBotTime, maxPairWithBotTime)
                delay(currentDelay)
                // check if we are still searching
                if (usersSearchingForGameJobsMap[userId] != null) {
                    println("no enemy was found for the user $userId, pairing with bot")
                    val botId = BotProvider.getBotFromBucket(queueToAddUser)
                    val gameData = GameData(
                        firstPlayerId = userId,
                        secondPlayerId = botId,
                        botId = botId
                    )
                    gamesRepository.create(gameData)
                    val gameId = gamesRepository.getGameIdByUserId(userId)!!
                    channel.send(Pair(false, gameId))
                    usersSearchingForGameJobsMap.remove(userId)
                }
            }
        }
        usersSearchingForGameJobsMap[userId] = job
    }

    init {
        bucketsOfUsersSearchingForGame.forEach { bucket ->
            CoroutineScope(searchingForGameScope).launch {
                while (true) {
                    if (bucket.isNotEmpty()) {
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
                    val gameData = GameData(
                        firstPlayerId = firstUser.first,
                        secondPlayerId = secondUser.first,
                        botId = null
                    )
                    gamesRepository.create(gameData)
                    val gameId = gamesRepository.getGameIdByUserId(firstUser.first)!!
                    listOf(firstUser, secondUser).forEach { (userId, channel) ->
                        channel.send(Pair(false, gameId))
                        usersSearchingForGameJobsMap.remove(userId)
                    }
                }
            }
        }
    }
}

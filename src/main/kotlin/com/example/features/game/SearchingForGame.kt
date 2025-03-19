/*
 * This file is part of nine-mens-morris-server (https://github.com/kroune/nine-mens-morris-server)
 * Copyright (C) 2024-2024  kroune
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact: kr0ne@tuta.io
 */
package com.example.features.game

import com.example.data.local.games.GameData
import com.example.data.local.gamesRepository
import com.example.data.local.queueRepository
import com.example.data.local.usersRepository
import com.example.features.ConfigurationLoader.currentConfig
import com.example.features.logging.bucketId
import com.example.features.logging.globalLogger
import com.example.features.logging.userId
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.random.Random

val minPairWithBotTime = currentConfig.gameConfig.minTimeBeforePairingWithBot
val maxPairWithBotTime = currentConfig.gameConfig.maxTimeBeforePairingWithBot
val bucketSize = currentConfig.gameConfig.bucketSize
val delayBeforeRecheckingBucket = currentConfig.gameConfig.delayBeforeRecheckingBucket

object SearchingForGame {
    private val userIdToSession = mutableMapOf<Long, Channel<Pair<Boolean, Long>>>()

    /**
     * this data isn't synchronized between pods, but that's ok,
     * since user either will be paired with another user (which will be written in db, so other pods will know this)
     * or he will be paired with bot, and it doesn't matter if bot will be requested by 1 pod or another
     */
    private val usersSearchingForGameJobsMap: MutableMap<Long, Job> = mutableMapOf()

    suspend fun removeUser(userId: Long) {
        usersSearchingForGameJobsMap[userId]?.cancel()
        queueRepository.deleteUser(userId)
    }

    suspend fun addUser(userId: Long, channel: Channel<Pair<Boolean, Long>>) {
        val rating = usersRepository.getRatingById(userId)!!
        val oldJob = usersSearchingForGameJobsMap[userId]
        userIdToSession[userId] = channel
        // cancel previous searching if it exists
        oldJob?.cancel()
        val job = CoroutineScope(Dispatchers.IO).launch {
            gamesRepository.getGameIdByUserId(userId)?.let { gameId ->
                // user is already in a game
                channel.send(Pair(false, gameId))
                channel.close()
                return@launch
            }
            globalLogger.atDebug {
                message = "Added user to the queue"
                payload = buildMap {
                    userId(userId)
                }
            }
            val queueToAddUser = (rating / bucketSize)
            val bucketsToSpreadBetween = currentConfig.gameConfig.maxRatingDifference / bucketSize
            val bucketsRange =
                (queueToAddUser - bucketsToSpreadBetween / 2)..(queueToAddUser + bucketsToSpreadBetween / 2)
            queueRepository.addUser(
                userId,
                bucketsRange
            )
            val currentDelay = Random.nextLong(minPairWithBotTime, maxPairWithBotTime)
            delay(currentDelay)
            // check if we are still searching
            globalLogger.atDebug {
                message = "delay before pairing with bot was waited delay = $currentDelay"
                payload = buildMap {
                    userId(userId)
                }
            }
            if (gamesRepository.getGameIdByUserId(userId) == null) {
                globalLogger.atDebug {
                    message = "game wasn't found after delay"
                    payload = buildMap {
                        userId(userId)
                    }
                }
                val botId = BotProvider.getBotFromBucket(bucketsRange.random())
                val gameData = GameData(
                    firstPlayerId = userId,
                    secondPlayerId = botId,
                    botId = botId
                )
                if (!gamesRepository.create(gameData)) {
                    // race condition, such game exists
                    globalLogger.atDebug {
                        message = "game was already created"
                        payload = buildMap {
                            userId(userId)
                        }
                    }
                    return@launch
                }
                val gameId = gamesRepository.getGameIdByUserId(userId)!!
                // make sure to initialize it, so time count starts
                GameDataFactory.getGame(gameId)
                channel.send(Pair(false, gameId))
            }
        }
        usersSearchingForGameJobsMap[userId] = job
    }

    private val searchingForGameScope = CoroutineScope(Dispatchers.IO)

    init {
        for (bucketId in 0..currentConfig.gameConfig.maxBucketNumber) {
            searchingForGameScope.launch {
                while (true) {
                    // if bucket number has changed
                    if (bucketId !in 0..currentConfig.gameConfig.maxBucketNumber) {
                        delay(delayBeforeRecheckingBucket)
                        continue
                    }
                    val availablePlayers = queueRepository.getUsers(bucketId).shuffled()
                    if (availablePlayers.isEmpty()) {
                        delay(delayBeforeRecheckingBucket)
                        continue
                    }
                    globalLogger.atDebug {
                        message = "bucket.size - ${availablePlayers.size}"
                        payload = buildMap {
                            bucketId(bucketId)
                        }
                    }
                    if (availablePlayers.size == 1) {
                        // TODO: add average game search time updater
                        val expectedWaitingTime = (10..20L).random()
                        userIdToSession[availablePlayers.first()]?.trySend(
                            Pair(true, expectedWaitingTime)
                        )
                        delay(delayBeforeRecheckingBucket)
                        continue
                    }
                    val firstUser = availablePlayers[0]
                    val secondUser = availablePlayers[1]
                    val gameData = GameData(
                        firstPlayerId = firstUser,
                        secondPlayerId = secondUser,
                        botId = null
                    )
                    if (!gamesRepository.create(gameData)) {
                        // race condition
                        continue
                    }
                    val gameId = gamesRepository.getGameIdByUserId(firstUser)!!
                    listOf(firstUser, secondUser).forEach { userId ->
                        userIdToSession[userId]?.trySend(Pair(false, gameId))
                        usersSearchingForGameJobsMap[userId]?.cancel()
                    }
                    // make sure to initialize it, so time count starts
                    GameDataFactory.getGame(gameId)
                }
            }
        }
    }
}

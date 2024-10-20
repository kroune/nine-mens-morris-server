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
import com.example.features.LogPriority
import com.example.features.currentConfig
import com.example.features.log
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
                return@launch
            }
            log("Added user to the queue $userId", LogPriority.Debug)
            val queueToAddUser = (rating / bucketSize)
            val bucketsToSpreadBetween = currentConfig.gameConfig.maxRatingDifference / bucketSize
            val bucketsRange = (queueToAddUser - bucketsToSpreadBetween / 2)..(queueToAddUser + bucketsToSpreadBetween / 2)
            queueRepository.addUser(
                userId,
                bucketsRange
            )
            val currentDelay = Random.nextLong(minPairWithBotTime, maxPairWithBotTime)
            delay(currentDelay)
            // check if we are still searching
            if (gamesRepository.getGameIdByUserId(userId) == null) {
                val botId = BotProvider.getBotFromBucket(bucketsRange.random())
                val gameData = GameData(
                    firstPlayerId = userId,
                    secondPlayerId = botId,
                    botId = botId
                )
                gamesRepository.create(gameData)
                val gameId = gamesRepository.getGameIdByUserId(userId)!!
                channel.send(Pair(false, gameId))
            }
        }
        usersSearchingForGameJobsMap[userId] = job
    }

    init {
        for (bucketId in 0..currentConfig.gameConfig.maxBucketNumber) {
            CoroutineScope(Dispatchers.IO).launch {
                while (true) {
                    val availablePlayers = ((bucketId - 5)..(bucketId + 5)).flatMap {
                        queueRepository.getUsers(it)
                    }.shuffled()
                    if (availablePlayers.isNotEmpty()) {
                        log("bucket.size - ${availablePlayers.size}", LogPriority.Debug)
                    }
                    if (availablePlayers.size == 1) {
                        // TODO: add average game search time updater
                        val expectedWaitingTime = 15L
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
                    gamesRepository.create(gameData)
                    val gameId = gamesRepository.getGameIdByUserId(firstUser)!!
                    listOf(firstUser, secondUser).forEach { userId ->
                        userIdToSession[userId]?.send(Pair(false, gameId))
                        usersSearchingForGameJobsMap[userId]?.cancel()
                    }
                }
            }
        }
    }
}

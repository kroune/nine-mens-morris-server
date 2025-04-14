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
package io.github.kroune.features.game

import io.github.kroune.data.local.games.GameData
import io.github.kroune.data.local.gamesRepository
import io.github.kroune.data.local.queueRepository
import io.github.kroune.data.local.usersRepository
import io.github.kroune.features.ConfigurationLoader.currentConfig
import io.github.kroune.features.logging.bucketId
import io.github.kroune.features.logging.globalLogger
import io.github.kroune.features.logging.userId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.koin.core.context.GlobalContext
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


private val consumer by lazy {
    val props by GlobalContext.get().inject<Properties>()
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.VoidSerializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.VoidDeserializer")
    props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 1.seconds.inWholeMilliseconds)
    props.put("group.id", "server")

    KafkaConsumer<String, Long>(props)
}

val minPairWithBotTime = currentConfig.gameConfig.minTimeBeforePairingWithBot
val maxPairWithBotTime = currentConfig.gameConfig.maxTimeBeforePairingWithBot
val bucketSize = currentConfig.gameConfig.bucketSize
val delayBeforeRecheckingBucket = currentConfig.gameConfig.delayBeforeRecheckingBucket

class SearchingForGameConnection(
    val expectedWaitingTime: MutableStateFlow<Long?>,
    val callback: suspend (Long) -> Unit
)

object SearchingForGame {
    private val userIdToSession = mutableMapOf<Long, SearchingForGameConnection>()

    /**
     * this data isn't synchronized between pods, but that's ok,
     * since user either will be paired with another user (which will be written in db, so other pods will know this)
     * or he will be paired with bot, and it doesn't matter if bot will be requested by 1 pod or another
     */
    private val usersSearchingForGameJobsMap: MutableMap<Long, Job> = mutableMapOf()

    suspend fun removeUser(userId: Long) {
        usersSearchingForGameJobsMap[userId]?.cancel()
        queueRepository.deleteUser(userId)
        globalLogger.atDebug {
            message = "Removed user from the queue"
            payload = buildMap {
                userId(userId)
            }
        }
    }

    suspend fun addUser(userId: Long, data: SearchingForGameConnection) {
        val rating = usersRepository.getRatingById(userId)!!
        val oldJob = usersSearchingForGameJobsMap[userId]
        userIdToSession[userId] = data
        // cancel previous searching if it exists
        oldJob?.cancel()
        val job = CoroutineScope(Dispatchers.IO).launch {
            gamesRepository.getGameIdByUserId(userId)?.let { gameId ->
                // user is already in a game
                data.callback(gameId)
                data.expectedWaitingTime.collect()
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
                data.callback(gameId)
            }
        }
        usersSearchingForGameJobsMap[userId] = job
    }

    private val searchingForGameScope = CoroutineScope(Dispatchers.IO)

    init {
        consumer.subscribe(Regex("searching-for-game-\\d*").toPattern())
        searchingForGameScope.launch {
            while (true) {
                val records = consumer.poll(5.seconds.toJavaDuration())
                    .map {
                        val topicName = it.topic()!!
                        topicName
                            .substringAfter("searching-for-game-")
                            .toInt()
                    }.toSet()
                records.forEach { bucketId ->
                    val availablePlayers = queueRepository.getUsers(bucketId).shuffled()
                    if (availablePlayers.isEmpty()) {
                        return@forEach
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
                        userIdToSession[availablePlayers.first()]?.expectedWaitingTime?.emit(
                            expectedWaitingTime
                        )
                        return@forEach
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
                        return@forEach
                    }
                    val gameId = gamesRepository.getGameIdByUserId(firstUser)!!
                    listOf(firstUser, secondUser).forEach { userId ->
                        userIdToSession[userId]?.callback(gameId)
                        usersSearchingForGameJobsMap[userId]?.cancel()
                    }
                    // make sure to initialize it, so time count starts
                    GameDataFactory.getGame(gameId)
                }
            }
        }
    }
}

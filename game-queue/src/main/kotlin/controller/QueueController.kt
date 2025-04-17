package controller

import BotProvider
import ConfigurationLoader.currentConfig
import GameDataFactory
import SearchingForGameConnection
import data.dao.GameData
import data.dao.GamesDataServiceI
import data.dao.UsersDataServiceI
import io.github.kroune.controller.GameController
import io.github.kroune.logging.globalLogger
import io.github.kroune.logging.userId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import queue.dao.QueueServiceI
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

private val producer by lazy {
    val props by GlobalContext.get().inject<Properties>()
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.VoidSerializer")

    KafkaProducer<String, Long>(props)
}

val minPairWithBotTime = currentConfig.gameConfig.minTimeBeforePairingWithBot
val maxPairWithBotTime = currentConfig.gameConfig.maxTimeBeforePairingWithBot
val bucketSize = currentConfig.gameConfig.bucketSize
val delayBeforeRecheckingBucket = currentConfig.gameConfig.delayBeforeRecheckingBucket

class QueueController(
    val datasource: QueueServiceI
) : KoinComponent {
    suspend fun addUser(userId: Long, data: SearchingForGameConnection) {
        val usersRepository by inject<UsersDataServiceI>()
        val rating = usersRepository.getRatingById(userId)!!
        val scope = CoroutineScope(coroutineContext)
        scope.launch {
            while (true) {
                // TODO: add average game search time updater
                val expectedWaitingTime = (15..22L).random()
                data.expectedWaitingTime.emit(expectedWaitingTime)
                delay(2.seconds)
            }
        }
        scope.launch {
            val gamesRepository by inject<GamesDataServiceI>()
            gamesRepository.getGameIdByUserId(userId)?.let { gameId ->
                // user is already in a game
                data.callback(gameId)
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


            val queueRepository by inject<QueueServiceI>()
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
    }

    suspend fun addUser(userId: Long, bucketRange: IntRange) {
        datasource.addUser(userId, bucketRange)
        bucketRange.forEach { bucket ->
            producer.send(ProducerRecord("searching-for-game-$bucket", null))
        }
    }

    suspend fun createGame(game: GameData): Boolean {
        val queueRepository by inject<QueueServiceI>()

        /**
         * firstPlayerId and secondPlayerId are shuffled, we delete them in such order, so that
         * if one deletion happens, the second one will be also performed there (since all other deletions
         * will get result equal to 0 ([queueRepository.deleteUser] has lock), so the second deletion won't happen)
         */
        val lowerId = min(game.firstPlayerId, game.secondPlayerId)
        val upperId = max(game.firstPlayerId, game.secondPlayerId)
        if ((game.botId != upperId && queueRepository.deleteUser(upperId) == 0) ||
            (game.botId != lowerId && queueRepository.deleteUser(lowerId) == 0)
        ) {
            return false
        }
        val gameController by inject<GameController>()
        return gameController.create(game)
    }

    suspend fun getUsers(bucket: Int): List<Long> {
        return datasource.getUsers(bucket = bucket)
    }


    /**
     * @return amount of deleted rows
     */
    suspend fun deleteUser(userId: Long): Int {
        return datasource.deleteUser(userId)
    }
}
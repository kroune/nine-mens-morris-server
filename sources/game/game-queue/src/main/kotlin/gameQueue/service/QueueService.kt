package gameQueue.service

import botsApi.BotProviderI
import common.ConfigurationLoader.currentConfig
import common.logging.bucketId
import common.logging.globalLogger
import common.logging.userId
import gameCommon.GameI
import gameCommon.data.dao.GameData
import gameCommon.data.dao.GamesDataServiceI
import gameQueue.data.queue.dao.QueueServiceI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import userApi.data.dao.UsersDataServiceI
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class QueueService(
    private val datasource: QueueServiceI,
    private val gamesRepository: GamesDataServiceI,
    private val usersRepository: UsersDataServiceI,
    private val botProvider: BotProviderI,
    private val queueRepository: QueueServiceI,
    private val gamesDataService: GamesDataServiceI,
) : KoinComponent {
    private val minPairWithBotTime = currentConfig.gameConfig.minTimeBeforePairingWithBot
    private val maxPairWithBotTime = currentConfig.gameConfig.maxTimeBeforePairingWithBot
    private val bucketSize = currentConfig.gameConfig.bucketSize

    private suspend fun streamExpectedWaitingTime(expectedWaitingTimeChannel: Channel<Long>) {
        CoroutineScope(currentCoroutineContext()).launch {
            while (true) {
                // TODO: add average game search time updater
                val expectedWaitingTime = (15..22L).random()
                expectedWaitingTimeChannel.send(expectedWaitingTime)
                delay(2.seconds)
            }
        }
    }

    suspend fun addUser(userId: Long, onGameFound: suspend (Long) -> Unit): Channel<Long> {
        val expectedWaitingTime = Channel<Long>(capacity = 50)
        CoroutineScope(currentCoroutineContext()).launch {
            val onGameFoundAndCleanup: suspend (Long) -> Unit = { expectedTime: Long ->
                onGameFound(expectedTime)
                expectedWaitingTime.cancel()
            }
            gamesRepository.getGameIdByUserId(userId)?.let { gameId ->
                // user is already in a game
                onGameFoundAndCleanup(gameId)
                return@launch
            }
            globalLogger.atDebug {
                message = "Added user to the queue"
                payload = buildMap {
                    userId(userId)
                }
            }
            userIdToSession[userId] = onGameFoundAndCleanup
            streamExpectedWaitingTime(expectedWaitingTime)

            val rating = usersRepository.getRatingById(userId)!!
            val queueToAddUser = (rating / bucketSize)
            val bucketsToSpreadBetween = currentConfig.gameConfig.maxRatingDifference / bucketSize
            val bucketsRange = (queueToAddUser - bucketsToSpreadBetween / 2)
                .coerceAtLeast(0)..(queueToAddUser + bucketsToSpreadBetween / 2)
            datasource.addUser(userId, bucketsRange)
            bucketsRange.forEach { bucket ->
                producer.send(ProducerRecord("searching-for-game-$bucket", null))
            }
            pairWithBotIfNoRealEnemyFoundInTime(
                userId,
                bucketsRange,
                onGameFoundAndCleanup,
            )
        }
        return expectedWaitingTime
    }

    suspend fun pairWithBotIfNoRealEnemyFoundInTime(
        userId: Long,
        bucketsRange: IntRange,
        onGameFound: suspend (Long) -> Unit,
    ) {
        val currentDelay = Random.nextLong(minPairWithBotTime, maxPairWithBotTime)
        delay(currentDelay)
        // check if we are still searching
        globalLogger.atDebug {
            message = "delay before pairing with bot was waited delay = $currentDelay"
            payload = buildMap {
                userId(userId)
            }
        }
        val gameId = gamesRepository.getGameIdByUserId(userId)
        if (gameId == null) {
            pairWithBot(userId, bucketsRange, onGameFound)
        }
    }

    suspend fun pairWithBot(userId: Long, bucketsRange: IntRange, onGameFound: suspend (Long) -> Unit) {
        globalLogger.atDebug {
            message = "game wasn't found after delay"
            payload = buildMap {
                userId(userId)
            }
        }
        val botId = botProvider.getBotFromBucket(bucketsRange.random())
        val gameData = GameData(
            firstPlayerId = userId,
            secondPlayerId = botId,
            botId = botId,
        )
        if (!gamesRepository.create(gameData)) {
            // race condition, such game exists
            globalLogger.atDebug {
                message = "game was already created"
                payload = buildMap {
                    userId(userId)
                }
            }
            return
        }
        val gameId = gamesRepository.getGameIdByUserId(userId)!!
        // make sure to initialize it, so time count starts
        val game by inject<GameI>(parameters = { parametersOf(gameId, null, null) })
        game.initializeGame()
        onGameFound(gameId)
    }

    suspend fun createGame(game: GameData): Boolean {
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
        return gamesDataService.create(game)
    }


    /**
     * @return amount of deleted rows
     */
    suspend fun removeUser(userId: Long): Int {
        globalLogger.atDebug {
            message = "Removed user from the queue"
            payload = buildMap {
                userId(userId)
            }
        }
        return datasource.deleteUser(userId)
    }


    private val consumer by lazy {
        val props by GlobalContext.get().inject<Properties>()
        props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
        props["value.serializer"] = "org.apache.kafka.common.serialization.VoidSerializer"
        props["value.deserializer"] = "org.apache.kafka.common.serialization.VoidDeserializer"
        props[CommonClientConfigs.METADATA_MAX_AGE_CONFIG] = 1.seconds.inWholeMilliseconds
        props["group.id"] = "server"

        KafkaConsumer<String, Long>(props)
    }

    private val producer by lazy {
        val props by GlobalContext.get().inject<Properties>()
        props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
        props["value.serializer"] = "org.apache.kafka.common.serialization.VoidSerializer"

        KafkaProducer<String, Long>(props)
    }

    init {
        consumer.subscribe(Regex("searching-for-game-\\d*").toPattern())
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                consumer.poll(5.seconds.toJavaDuration())
                    .map {
                        val topicName = it.topic()!!
                        topicName
                            .substringAfter("searching-for-game-")
                            .toInt()
                    }
                    .toSet()
                    .forEach { bucketId ->
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
                        if (availablePlayers.size < 2) {
                            return@forEach
                        }
                        val firstUser = availablePlayers[0]
                        val secondUser = availablePlayers[1]
                        val gameData = GameData(
                            firstPlayerId = firstUser,
                            secondPlayerId = secondUser,
                            botId = null
                        )
                        if (!createGame(gameData)) {
                            // race condition
                            return@forEach
                        }
                        val gameId = gamesRepository.getGameIdByUserId(firstUser)!!
                        listOf(firstUser, secondUser).forEach { userId ->
                            launch {
                                userIdToSession[userId]?.invoke(gameId)
                            }
                        }
                        // make sure to initialize it, so time count starts
                        val game by inject<GameI>(parameters = {
                            parametersOf(gameId, null, null)
                        })
                        game.initializeGame()
                    }
            }
        }
    }

    companion object {
        private val userIdToSession = mutableMapOf<Long, suspend (Long) -> Unit>()
    }
}
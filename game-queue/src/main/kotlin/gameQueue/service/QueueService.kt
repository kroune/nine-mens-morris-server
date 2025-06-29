package gameQueue.service

import BotProvider
import common.ConfigurationLoader.currentConfig
import common.logging.bucketId
import common.logging.globalLogger
import common.logging.userId
import gameMain.Game
import gameMain.data.dao.GameData
import gameMain.data.dao.GamesDataServiceI
import gameQueue.data.queue.dao.QueueServiceI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import user.data.dao.UsersDataServiceI
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


class SearchingForGameConnection(
    val expectedWaitingTime: MutableStateFlow<Long?>,
    val callback: suspend (Long) -> Unit
)

class QueueService(
    val datasource: QueueServiceI
) : KoinComponent {
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

    private val producer by lazy {
        val props by GlobalContext.get().inject<Properties>()
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        props.put("value.serializer", "org.apache.kafka.common.serialization.VoidSerializer")

        KafkaProducer<String, Long>(props)
    }

    private val minPairWithBotTime = currentConfig.gameConfig.minTimeBeforePairingWithBot
    private val maxPairWithBotTime = currentConfig.gameConfig.maxTimeBeforePairingWithBot
    private val bucketSize = currentConfig.gameConfig.bucketSize

    companion object {
        private val userIdToSession = mutableMapOf<Long, SearchingForGameConnection>()
    }

    private val gamesRepository by inject<GamesDataServiceI>()

    private suspend fun streamExpectedWaitingTime(expectedWaitingTimeChannel: MutableStateFlow<Long?>) {
        while (true) {
            // TODO: add average game search time updater
            val expectedWaitingTime = (15..22L).random()
            expectedWaitingTimeChannel.emit(expectedWaitingTime)
            delay(2.seconds)
        }
    }

    suspend fun addUser(userId: Long, data: SearchingForGameConnection) {
        val usersRepository by inject<UsersDataServiceI>()
        val rating = usersRepository.getRatingById(userId)!!
        val scope = CoroutineScope(coroutineContext)
        userIdToSession[userId] = data
        scope.launch {
            streamExpectedWaitingTime(data.expectedWaitingTime)
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
                (queueToAddUser - bucketsToSpreadBetween / 2).coerceAtLeast(0)..(queueToAddUser + bucketsToSpreadBetween / 2)

            addUser(
                userId,
                bucketsRange
            )
            pairWithBotIfNoRealEnemyFoundInTime(
                userId,
                bucketsRange,
                data
            )
        }
    }

    suspend fun pairWithBotIfNoRealEnemyFoundInTime(
        userId: Long,
        bucketsRange: IntRange,
        data: SearchingForGameConnection
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
            pairWithBot(userId, bucketsRange, data)
        }
    }

    suspend fun pairWithBot(userId: Long, bucketsRange: IntRange, data: SearchingForGameConnection) {
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
            return
        }
        val gameId = gamesRepository.getGameIdByUserId(userId)!!
        // make sure to initialize it, so time count starts
        Game(gameId).initializeGame()
        data.callback(gameId)
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
        val gamesDataService by inject<GamesDataServiceI>()
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


    private val searchingForGameScope = CoroutineScope(Dispatchers.IO)

    init {
        searchingForGameScope.launch {
            consumer.subscribe(Regex("searching-for-game-\\d*").toPattern())
            val queueRepository by inject<QueueServiceI>()
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
                            userIdToSession[userId]?.callback(gameId)
                        }
                    }
                    // make sure to initialize it, so time count starts
                    Game(gameId).initializeGame()
                }
            }
        }
    }
}
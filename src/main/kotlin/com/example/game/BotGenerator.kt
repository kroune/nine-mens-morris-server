package com.example.game

import com.example.LogPriority
import com.example.data.botFactory
import com.example.data.users.UserData
import com.example.data.usersRepository
import com.example.log
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random
import kotlin.random.nextInt

object BotGenerator {
    private val botRepository = botFactory
    /**
     * array of buckets, represented by queue of user ids
     */
    private val availableBotsBuckets: Array<Queue<Long>> = Array(50) {
        ConcurrentLinkedQueue()
    }
    private val network = HttpClient()
    private val jsonClient = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun botGotFree(login: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val botRating = usersRepository.getRatingByLogin(login)!!
            val queueToAddBot = (botRating / bucketSize)
            val id = usersRepository.getIdByLogin(login)!!
            availableBotsBuckets[queueToAddBot].add(id)
            log("bot got free $login", LogPriority.Debug)
        }
    }

    suspend fun isBot(login: String): Boolean {
        val id = usersRepository.getIdByLogin(login)!!
        return botRepository.get(id)
    }

    suspend fun getBotFromBucket(bucket: Int): Long {
        return availableBotsBuckets[bucket].poll() ?: createBot(bucket * bucketSize..bucket * (bucketSize + 1))
    }

    private suspend fun createBot(rating: IntRange = 0..1000): Long {
        val result = network.get("https://randomuser.me/api/?inc=login,picture").bodyAsText()
        val serviceData = jsonClient.decodeFromString<ServiceResponse>(result)
        val login = serviceData.username()
        val password = getRandomString(10)
        val botRating = Random.nextInt(rating)

        if (usersRepository.isLoginPresent(login)) {
            log("creating bot failed $login", LogPriority.Debug)
            return createBot()
        }

        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val data = UserData(login, password, currentDate)
        usersRepository.create(data)

        usersRepository.updateRatingByLogin(login, botRating)
        val profilePicture = network.get(serviceData.pictureUrl()).readBytes()
        usersRepository.updatePictureByLogin(login, profilePicture)
        val queueToAddBot = (botRating / bucketSize)
        val id = usersRepository.getIdByLogin(login)!!
        botRepository.add(id)
        availableBotsBuckets[queueToAddBot].add(id)
        log("created bot with $login $password", LogPriority.Debug)
        return id
    }
}

@Serializable
private class ServiceResponse(val results: Array<results>) {
    fun username(): String {
        return results[0].login.username
    }

    fun pictureUrl(): String {
        return results[0].picture.medium
    }
}

@Serializable
private class results(val login: login, val picture: picture)

@Serializable
private class login(val username: String)

@Serializable
private class picture(val medium: String)

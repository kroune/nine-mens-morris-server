package com.example.game

import com.example.users.Users
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random
import kotlin.random.nextLong

object BotGenerator {
    /**
     * array of buckets, represented by queue from [Pair] of [Connection] and [Channel]
     */
    private val availableBotsBuckets: Array<Queue<Pair<String, String>>> = Array(50) {
        ConcurrentLinkedQueue()
    }
    private val botsList = hashMapOf<String, String>()
    private val network = HttpClient()
    private val jsonClient = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun botGotFree(login: String) {
        val botRating = Users.getRatingByLogin(login).getOrThrow()
        val queueToAddBot = (botRating / bucketSize).toInt()
        availableBotsBuckets[queueToAddBot].add(Pair(login, botsList[login]!!))
    }

    fun isBot(login: String): Boolean {
        return botsList[login] != null
    }
    suspend fun getBotFromBucket(bucket: Int): Pair<String, String> {
        return availableBotsBuckets[bucket].poll() ?: createBot(bucket * bucketSize..bucket * (bucketSize + 1L))
    }

    suspend fun createBot(rating: LongRange = 0..1000L): Pair<String, String> {
        val result = network.get("https://randomuser.me/api/?inc=login,picture").bodyAsText()
        val serviceData = jsonClient.decodeFromString<ServiceResponse>(result)
        val login = serviceData.username()
        val password = getRandomString(10)
        val botRating = Random.nextLong(rating)
        val registerAttempt = Users.register(serviceData.username(), password)
        if (registerAttempt.isFailure) {
            return createBot()
        }
        Users.setRatingByLogin(login, botRating)
        val profilePicture = network.get(serviceData.pictureUrl()).readBytes()
        Users.uploadPictureByLogin(login, profilePicture)
        botsList[login] = password
        val queueToAddBot = (botRating / bucketSize).toInt()
        availableBotsBuckets[queueToAddBot].add(Pair(login, password))
        save()
        return Pair(login, password)
    }

    private fun save() {
        botsDataDir.mkdirs()
        run {
            val botsFile = File(botsDataDir, "botsNames")
            botsFile.createNewFile()
            val encodedText = jsonClient.encodeToString(botsList)
            botsFile.writeText(encodedText)
        }
    }

    private val botsDataDir = File("data/bots")

    init {
        botsDataDir.mkdirs()
        run {
            val botsFile = File(botsDataDir, "botsNames")
            botsFile.createNewFile()
            val text = botsFile.readText()
            val botsListFromFile = Json.decodeFromString<MutableList<Pair<String, String>>>(text)
            botsListFromFile.forEach { (login, password) ->
                botsList[login] = password
                val rating = Users.getRatingByLogin(login).getOrThrow()
                val queueToAddBot = (rating / bucketSize).toInt()
                availableBotsBuckets[queueToAddBot].add(Pair(login, password))
            }
        }
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

fun main() {
    runBlocking {
        BotGenerator.createBot()
    }
}

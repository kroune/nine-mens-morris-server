package com.example.game

import com.example.LogPriority
import com.example.currentConfig
import com.example.log
import com.example.users.Users
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.channels.Channel
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
        log("bot got free $login", LogPriority.Debug)
    }

    fun isBot(login: String): Boolean {
        return botsList[login] != null
    }
    suspend fun getBotFromBucket(bucket: Int): Pair<String, String> {
        return availableBotsBuckets[bucket].poll() ?: createBot(bucket * bucketSize..bucket * (bucketSize + 1L))
    }

    private suspend fun createBot(rating: LongRange = 0..1000L): Pair<String, String> {
        val result = network.get("https://randomuser.me/api/?inc=login,picture").bodyAsText()
        val serviceData = jsonClient.decodeFromString<ServiceResponse>(result)
        val login = serviceData.username()
        val password = getRandomString(10)
        val botRating = Random.nextLong(rating)
        val registerAttempt = Users.register(serviceData.username(), password)
        if (registerAttempt.isFailure) {
            log("creating bot failed $login", LogPriority.Debug)
            return createBot()
        }
        Users.setRatingByLogin(login, botRating)
        val profilePicture = network.get(serviceData.pictureUrl()).readBytes()
        Users.uploadPictureByLogin(login, profilePicture)
        botsList[login] = password
        val queueToAddBot = (botRating / bucketSize).toInt()
        availableBotsBuckets[queueToAddBot].add(Pair(login, password))
        save()
        log("created bot with $login $password", LogPriority.Debug)
        return Pair(login, password)
    }

    private fun save() {
        botsDataDir.mkdirs()
        run {
            val botsFile = File(botsDataDir, "botsNames.json")
            botsFile.createNewFile()
            val botsListAsList = botsList.map { (login, password) ->
                Pair(login, password)
            }
            val encodedText = jsonClient.encodeToString(botsListAsList)
            botsFile.writeText(encodedText)
        }
    }

    private val botsDataDir = File(currentConfig.fileConfig.botsDataDir)

    init {
        botsDataDir.mkdirs()
        run {
            val botsFile = File(botsDataDir, "botsNames.json")
            if (botsFile.createNewFile()) {
                botsFile.writeText("[]")
            }
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

package com.example.game

import com.example.LogPriority
import com.example.api.randomUserRepository
import com.example.common.getRandomString
import com.example.data.botsRepository
import com.example.data.users.UserData
import com.example.data.usersRepository
import com.example.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random
import kotlin.random.nextInt

object BotProvider {
    const val BUCKETS_AMOUNT = 50
    /**
     * array of buckets, represented by queue of user ids
     */
    private val availableBotsBuckets: Array<Queue<Long>> = Array(BUCKETS_AMOUNT) {
        ConcurrentLinkedQueue()
    }

    fun botGotFree(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val botRating = usersRepository.getRatingById(id)!!
            val queueToAddBot = (botRating / bucketSize)
            availableBotsBuckets[queueToAddBot].add(id)
            log("bot got free $id [id]", LogPriority.Debug)
        }
    }

    suspend fun isBot(id: Long): Boolean {
        return botsRepository.get(id)
    }

    suspend fun getBotFromBucket(bucket: Int): Long {
        return availableBotsBuckets[bucket].poll() ?: createBot(bucket * bucketSize..bucket * (bucketSize + 1))
    }

    private suspend fun createBot(ratingRange: IntRange = 0..1000): Long {
        val login: String
        val picture: ByteArray
        run {
            repeat(10) {
                val (loginVariant, pictureVariant) = randomUserRepository.getLoginAndPicture().getOrElse {
                    return@repeat
                }

                if (usersRepository.isLoginPresent(loginVariant)) {
                    return@repeat
                }
                login = loginVariant
                picture = pictureVariant
                return@run
            }
            error("no valid login + picture found")
        }

        val password = getRandomString(16)
        val rating = Random.nextInt(ratingRange)
        val data = UserData(
            login = login,
            password = password,
            profilePicture = picture,
            rating = rating
        )

        usersRepository.create(data)
        val id = usersRepository.getIdByLogin(login)!!
        botsRepository.add(id)
        val queueToAddBot = (rating / bucketSize)
        availableBotsBuckets[queueToAddBot].add(id)
        log("created bot with $login $password", LogPriority.Debug)
        return id
    }
}

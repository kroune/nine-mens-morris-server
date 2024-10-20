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

import com.example.features.LogPriority
import com.example.data.local.botsRepository
import com.example.data.local.usersRepository
import com.example.features.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object BotProvider {
    const val BUCKETS_AMOUNT = 50
    /**
     * array of buckets, represented by queue of user ids
     */
    private val availableBotsBuckets: Array<Queue<Long>> = Array(BUCKETS_AMOUNT) {
        ConcurrentLinkedQueue()
    }

    fun addBotToTheFreeBotsQueue(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val botRating = usersRepository.getRatingById(id)!!
            val queueToAddBot = (botRating / bucketSize)
            availableBotsBuckets[queueToAddBot].add(id)
            log("bot got free $id [id]", LogPriority.Debug)
        }
    }

    suspend fun isBot(id: Long): Boolean {
        return botsRepository.exists(id)
    }

    /**
     * @return id of the bot
     */
    suspend fun getBotFromBucket(bucket: Int): Long {
        return availableBotsBuckets[bucket].poll() ?: run {
            val id = BotCreator.createBot(bucket * bucketSize..bucket * (bucketSize + 1))
            id
        }
    }
}

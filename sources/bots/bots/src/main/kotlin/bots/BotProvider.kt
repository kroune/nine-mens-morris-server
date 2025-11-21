package bots
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

import botsApi.BotCreatorI
import botsApi.BotProviderI
import botsApi.dao.BotsServiceI
import common.ConfigurationLoader.currentConfig
import common.logging.bucketId
import common.logging.globalLogger
import common.logging.userId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import userApi.data.dao.UsersDataServiceI
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

private val bucketSize = currentConfig.gameConfig.bucketSize

internal class BotProvider(
    private val usersRepository: UsersDataServiceI,
    private val botsCreator: BotCreatorI,
    private val botsRepository: BotsServiceI,
) : BotProviderI, KoinComponent {

    /**
     * array of buckets, represented by queue of user ids
     */
    private val availableBotsBuckets: Array<Queue<Long>> = Array(BUCKETS_AMOUNT) {
        ConcurrentLinkedQueue()
    }

    override fun addBotToTheFreeBotsQueue(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val botRating = usersRepository.getRatingById(id)!!
            val queueToAddBot = (botRating / bucketSize)
            availableBotsBuckets[queueToAddBot].add(id)
            globalLogger.atDebug {
                message = "bot got free"
                payload = buildMap {
                    userId(id)
                }
            }
        }
    }

    override suspend fun isBot(id: Long): Boolean {
        return botsRepository.exists(id)
    }

    /**
     * @return id of the bot
     */
    override suspend fun getBotFromBucket(bucket: Int): Long {
        globalLogger.atInfo {
            message = "getting bot from bucket"
            payload = buildMap {
                bucketId(bucket)
            }
        }
        return availableBotsBuckets[bucket].poll() ?: run {
            val id = botsCreator.createBot(bucket * bucketSize..bucket * (bucketSize + 1))
            id
        }
    }

    companion object {
        private const val BUCKETS_AMOUNT = 50
    }
}

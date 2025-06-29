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

import bots.dao.BotsServiceI
import common.getRandomString
import common.logging.globalLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import randomUser.RandomUserRepositoryI
import user.data.dao.UsersDataServiceI
import user.domain.UsersService
import user.model.InsertUserPayload
import kotlin.random.Random
import kotlin.random.nextInt

object BotCreator : KoinComponent {
    suspend fun createBot(ratingRange: IntRange = 0..1000): Long {
        val login: String
        val picture: ByteArray?
        run {
            repeat(3) {
                val randomUserRepository by inject<RandomUserRepositoryI>()
                val (loginVariant, pictureVariant) = randomUserRepository.getLoginAndPicture().getOrElse {
                    globalLogger.atError {
                        message = "Failed creating bot"
                        cause = it
                    }
                    return@repeat
                }

                val usersRepository by inject<UsersDataServiceI>()
                if (usersRepository.isLoginPresent(loginVariant)) {
                    return@repeat
                }
                login = loginVariant
                picture = pictureVariant
                return@run
            }
            login = getRandomString(7)
            picture = null
        }

        val password = getRandomString(16)
        val usersService by inject<UsersService>()
        val rating = Random.nextInt(ratingRange)
        usersService.register(
            InsertUserPayload(
                login = login,
                password = password,
                profilePicture = picture,
                rating = rating
            )
        )

        val usersRepository by inject<UsersDataServiceI>()
        val id = usersRepository.getIdByLogin(login)!!

        val botsRepository by inject<BotsServiceI>()
        botsRepository.add(id)
        globalLogger.debug { "created bot with $login $password" }
        return id
    }
}
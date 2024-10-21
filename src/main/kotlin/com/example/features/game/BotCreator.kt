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

import com.example.data.remote.randomUserRepository
import com.example.common.getRandomString
import com.example.data.local.botsRepository
import com.example.data.local.users.UserData
import com.example.data.local.usersRepository
import com.example.features.logging.log
import io.opentelemetry.api.logs.Severity
import kotlin.random.Random
import kotlin.random.nextInt

object BotCreator {
    suspend fun createBot(ratingRange: IntRange = 0..1000): Long {
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
        log("created bot with $login $password", Severity.DEBUG)
        return id
    }
}
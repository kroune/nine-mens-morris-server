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
package com.example.data.users.repository

import com.example.data.users.UserData
import kotlinx.datetime.LocalDate

interface UsersDataRepositoryI {
    suspend fun create(data: UserData)
    suspend fun getIdByLogin(login: String): Long?
    suspend fun getLoginById(id: Long): String?
    suspend fun updatePictureByLogin(login: String, newPicture: ByteArray)
    suspend fun updatePictureById(id: Long, newPicture: ByteArray)
    suspend fun getPictureById(id: Long): ByteArray?
    suspend fun getRatingByLogin(login: String): Int?
    suspend fun getRatingById(id: Long): Int?
    suspend fun getCreationDateById(id: Long): LocalDate?
    suspend fun updateRatingByLogin(login: String, newRating: Int)
    suspend fun updateRatingById(id: Long, newRating: Int)
    suspend fun getIdByJwtToken(jwtToken: String): Long?
    suspend fun exists(login: String, password: String): Boolean
    suspend fun isLoginPresent(login: String): Boolean
}
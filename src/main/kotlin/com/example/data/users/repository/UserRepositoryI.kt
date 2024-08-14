package com.example.data.users.repository

import com.example.data.users.UserData
import kotlinx.datetime.LocalDate

interface UserRepositoryI {
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
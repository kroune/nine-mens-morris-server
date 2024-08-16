package com.example.api.randomUser

interface RandomUserRepositoryI {
    suspend fun getLoginAndPicture(): Result<Pair<String, ByteArray>>
}
package com.example.data.bots.repository

interface BotRepositoryI {
    suspend fun add(id: Long)
    suspend fun get(id: Long): Boolean
}
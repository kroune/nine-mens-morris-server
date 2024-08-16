package com.example.data.bots.repository

interface BotsRepositoryI {
    suspend fun add(id: Long)
    suspend fun get(id: Long): Boolean
}
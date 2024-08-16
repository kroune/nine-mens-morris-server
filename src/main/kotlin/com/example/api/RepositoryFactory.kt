package com.example.api

import com.example.api.randomUser.RandomUserRepositoryI
import com.example.api.randomUser.RandomUserRepositoryImpl

val randomUserRepository: RandomUserRepositoryI = RandomUserRepositoryImpl()
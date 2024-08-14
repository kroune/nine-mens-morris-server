package com.example.data

import com.example.data.bots.repository.BotRepositoryI
import com.example.data.bots.repository.BotRepositoryImpl
import com.example.data.users.repository.UserRepositoryI
import com.example.data.users.repository.UserRepositoryImpl

val usersRepository: UserRepositoryI = UserRepositoryImpl()
val botFactory: BotRepositoryI = BotRepositoryImpl()
package com.example.data

import com.example.data.bots.repository.BotsRepositoryI
import com.example.data.bots.repository.BotsRepositoryImpl
import com.example.data.games.repository.GamesDataRepositoryI
import com.example.data.games.repository.GamesDataRepositoryImpl
import com.example.data.users.repository.UsersDataRepositoryI
import com.example.data.users.repository.UsersDataRepositoryImpl

val usersRepository: UsersDataRepositoryI = UsersDataRepositoryImpl()
val botsRepository: BotsRepositoryI = BotsRepositoryImpl()
val gamesRepository: GamesDataRepositoryI = GamesDataRepositoryImpl()
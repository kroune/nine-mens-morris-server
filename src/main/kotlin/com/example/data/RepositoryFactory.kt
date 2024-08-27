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
package com.example.data

import com.example.data.bots.repository.BotsRepositoryI
import com.example.data.bots.repository.BotsRepositoryImpl
import com.example.data.games.repository.GamesDataRepositoryI
import com.example.data.games.repository.GamesDataRepositoryImpl
import com.example.data.queue.repository.QueueRepositoryI
import com.example.data.queue.repository.QueueRepositoryImpl
import com.example.data.users.repository.UsersDataRepositoryI
import com.example.data.users.repository.UsersDataRepositoryImpl

val usersRepository: UsersDataRepositoryI = UsersDataRepositoryImpl()
val botsRepository: BotsRepositoryI = BotsRepositoryImpl()
val gamesRepository: GamesDataRepositoryI = GamesDataRepositoryImpl()
val queueRepository: QueueRepositoryI = QueueRepositoryImpl()
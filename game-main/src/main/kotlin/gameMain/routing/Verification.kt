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
package gameMain.routing

import gameMain.data.dao.GamesDataServiceI
import user.routing.gameIdIsNotLong
import user.routing.gameIdIsNotValid
import user.routing.noGameId
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.koin.core.context.GlobalContext


/**
 * possible responses:
 *
 * [noGameId]
 *
 * [gameIdIsNotLong]
 *
 * [gameIdIsNotValid]
 *
 * [Nothing]
 */
suspend inline fun DefaultWebSocketServerSession.requireGameId(): Long? {
    val gameId = call.parameters["gameId"]
    if (gameId == null) {
        noGameId()
        return null
    }
    val gameIdToLong = gameId.toLongOrNull()
    if (gameIdToLong == null) {
        gameIdIsNotLong()
        return null
    }
    val koin = GlobalContext.get()
    val gamesRepository by koin.inject<GamesDataServiceI>()
    val gameExists = gamesRepository.exists(gameIdToLong)
    if (!gameExists) {
        gameIdIsNotValid()
        return null
    }
    return gameIdToLong
}

/**
 * possible responses:
 *
 * [noGameId]
 *
 * [gameIdIsNotLong]
 *
 * [gameIdIsNotValid]
 *
 * [Nothing]
 */
suspend inline fun RoutingContext.requireGameId(lambda: () -> Unit) {
    val gameId = call.parameters["gameId"]
    if (gameId == null) {
        noGameId()
        lambda()
        return
    }
    if (gameId.toLongOrNull() == null) {
        gameIdIsNotLong()
        lambda()
        return
    }
    val koin = GlobalContext.get()
    val gamesRepository by koin.inject<GamesDataServiceI>()
    val gameExists = gamesRepository.exists(gameId.toLong())
    if (!gameExists) {
        gameIdIsNotValid()
        lambda()
        return
    }
}

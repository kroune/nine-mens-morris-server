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
package gameMain.controller

import gameCommon.data.dao.GamesDataServiceI
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import userApi.controller.requireValidJwtToken
import userApi.view.internalServerError
import userApi.data.dao.UsersDataServiceI

fun Route.gameRoutingGET() {
    /**
     * 400 = incorrect jwt token
     * null = user isn't playing currently
     * [Long] = game id
     *
     * 500 = internal server error
     */
    get("/is-playing") {
        requireValidJwtToken {
            return@get
        }
        val jwtToken = call.parameters["jwtToken"]!!
        val usersRepository by inject<UsersDataServiceI>()
        val userId = usersRepository.getIdByJwtToken(jwtToken) ?: run {
            internalServerError()
            return@get
        }
        val gamesRepository by inject<GamesDataServiceI>()
        val gameId = gamesRepository.getGameIdByUserId(userId)
        call.respondNullable<Long?>(gameId)
    }
}

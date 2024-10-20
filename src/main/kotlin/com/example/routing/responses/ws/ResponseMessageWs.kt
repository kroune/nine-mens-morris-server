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
package com.example.routing.responses.ws

import io.ktor.server.websocket.*
import io.ktor.websocket.*

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.someThingsWentWrong(message: String) {
    close(CloseReason(406_0, "Something went wrong $message"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.jwtTokenIsNotValidForThisGame() {
    close(CloseReason(400_0, "this [jwtToken] isn't valid for this game"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.noGameId() {
    close(CloseReason(400_0, "no [gameId] parameter found"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.gameIdIsNotValid() {
    close(CloseReason(400_0, "[gameId] parameter is not valid"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.gameIdIsNotLong() {
    close(CloseReason(400_0, "[gameId] parameter is not a valid representation of a number"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.noJwtToken() {
    close(CloseReason(400_0, "no [jwtToken] parameter found"))
}

/**
 * closes web socket connection
 */
suspend inline fun DefaultWebSocketServerSession.jwtTokenIsNotValid() {
    close(CloseReason(403_0, "[jwtToken] parameter is not valid"))
}

suspend inline fun DefaultWebSocketServerSession.internalServerError() {
    close(CloseReason(500_0, "Internal server error"))
}
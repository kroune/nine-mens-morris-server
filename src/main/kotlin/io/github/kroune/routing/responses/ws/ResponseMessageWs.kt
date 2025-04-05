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
package io.github.kroune.routing.responses.ws

import io.github.kroune.features.logging.globalLogger
import io.ktor.server.websocket.*
import io.ktor.websocket.*

/**
 * closes web socket connection
 */
suspend inline fun WebSocketServerSession.someThingsWentWrong(cause: String) {
    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Something went wrong $cause"))
    globalLogger.atInfo {
        message = "Something went wrong $cause"
    }
}

/**
 * closes web socket connection
 */
suspend inline fun WebSocketServerSession.jwtTokenIsNotValidForThisGame() {
    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "this [jwtToken] isn't valid for this game"))
    globalLogger.atInfo {
        message = "Create new scratch file from selection"
    }
}

/**
 * closes web socket connection
 */
suspend inline fun WebSocketServerSession.noGameId() {
    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "no [gameId] parameter found"))
    globalLogger.atInfo {
        message = "no [gameId] parameter found"
    }
}

/**
 * closes web socket connection
 */
suspend inline fun WebSocketServerSession.gameIdIsNotValid() {
    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "[gameId] parameter is not valid"))
    globalLogger.atInfo {
        message = "[gameId] parameter is not valid"
    }
}

/**
 * closes web socket connection
 */
suspend inline fun WebSocketServerSession.gameIdIsNotLong() {
    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "[gameId] parameter is not a valid representation of a number"))
    globalLogger.atInfo {
        message = "[gameId] parameter is not a valid representation of a number"
    }
}

/**
 * closes web socket connection
 */
suspend inline fun WebSocketServerSession.noJwtToken() {
    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "no [jwtToken] parameter found"))
    globalLogger.atInfo {
        message = "no [jwtToken] parameter found"
    }
}

/**
 * closes web socket connection
 */
suspend inline fun WebSocketServerSession.jwtTokenIsNotValid() {
    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "[jwtToken] parameter is not valid"))
    globalLogger.atInfo {
        message = "[jwtToken] parameter is not valid"
    }
}

suspend inline fun WebSocketServerSession.internalServerError() {
    close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Internal server error"))
    globalLogger.atInfo {
        message = "Internal server error"
    }
}
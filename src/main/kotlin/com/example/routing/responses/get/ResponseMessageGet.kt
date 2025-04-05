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
package com.example.routing.responses.get

import com.example.features.ConfigurationLoader.currentConfig
import com.example.features.logging.globalLogger
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingContext.noJwtToken() {
    call.respond(HttpStatusCode.BadRequest.description("no [jwtToken] parameter found"))
    globalLogger.atInfo { "no [jwtToken] parameter found" }
}

suspend inline fun RoutingContext.jwtTokenIsNotValid() {
    call.respond(HttpStatusCode.Forbidden.description("[jwtToken] parameter is not valid"))
    globalLogger.atInfo { "[jwtToken] parameter is not valid" }
}

suspend fun RoutingContext.noLogin() {
    call.respond(HttpStatusCode.BadRequest.description("no [login] parameter found"))
    globalLogger.atInfo { "no [login] parameter found" }
}

/**
 * closes web socket connection
 */
suspend inline fun RoutingContext.noGameId() {
    call.respond(HttpStatusCode.BadRequest.description("no [gameId] parameter found"))
    globalLogger.atInfo {
        message = "no [gameId] parameter found"
    }
}

/**
 * closes web socket connection
 */
suspend inline fun RoutingContext.gameIdIsNotLong() {
    call.respond(HttpStatusCode.BadRequest.description("[gameId] parameter is not a valid representation of a number"))
    globalLogger.atInfo {
        message = "[gameId] parameter is not a valid representation of a number"
    }
}

/**
 * closes web socket connection
 */
suspend inline fun RoutingContext.gameIdIsNotValid() {
    call.respond(HttpStatusCode.BadRequest.description("[gameId] parameter is not valid"))
    globalLogger.atInfo {
        message = "[gameId] parameter is not valid"
    }
}

/**
 * closes web socket connection
 */
suspend inline fun RoutingContext.jwtTokenIsNotValidForThisGame() {
    call.respond(HttpStatusCode.BadRequest.description("this [jwtToken] isn't valid for this game"))
    globalLogger.atInfo {
        message = "Create new scratch file from selection"
    }
}

suspend fun RoutingContext.invalidLogin() {
    call.respond(HttpStatusCode.BadRequest.description("invalid [login] parameter found"))
    globalLogger.atInfo {
        message = "invalid [login] parameter found"
    }
}

suspend fun RoutingContext.noValidLogin() {
    call.respond(HttpStatusCode.BadRequest.description("no [login] parameter found"))
    globalLogger.atInfo { "no [login] parameter found" }
}

suspend fun RoutingContext.invalidPassword() {
    call.respond(HttpStatusCode.BadRequest.description("invalid [password] parameter found"))
    globalLogger.atInfo { "invalid [password] parameter found" }
}

suspend fun RoutingContext.noPassword() {
    call.respond(HttpStatusCode.BadRequest.description("no [password] parameter found"))
    globalLogger.atInfo { "no [password] parameter found" }
}

suspend fun RoutingContext.noUserId() {
    call.respond(HttpStatusCode.BadRequest.description("no [id] parameter found"))
    globalLogger.atInfo { "no [id] parameter found" }
}

suspend fun RoutingContext.userIdIsNotLong() {
    call.respond(HttpStatusCode.Forbidden.description("[id] parameter is not a long"))
    globalLogger.atInfo {
        message = "[id] parameter is not a long"
    }
}

suspend fun RoutingContext.userIdIsNotValid() {
    call.respond(HttpStatusCode.Forbidden.description("[id] parameter is not valid"))
    globalLogger.atInfo {
        message = "[id] parameter is not valid"
    }
}

suspend inline fun RoutingContext.imageIsTooLarge() {
    with(currentConfig.fileConfig) {
        call.respond(
            HttpStatusCode.Forbidden.description("provided image (byte array) is too large, it can be ${profilePictureMaxSize}x$profilePictureMaxSize at max"),
            "${profilePictureMaxSize}x$profilePictureMaxSize"
        )
        globalLogger.atInfo {
            message =
                "provided image (byte array) is too large, it can be ${profilePictureMaxSize}x$profilePictureMaxSize at max"
        }
    }
}

suspend inline fun RoutingContext.imageIsNotValid() {
    call.respond(HttpStatusCode.Forbidden.description("provided image (byte array) is not valid"))
    globalLogger.atInfo {
        message = "provided image (byte array) is not valid"
    }
}

suspend inline fun RoutingContext.internalServerError() {
    call.respond(HttpStatusCode.InternalServerError.description("Internal server error"))
    globalLogger.atError {
        message = "Internal server error"
    }
}

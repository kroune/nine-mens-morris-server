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

import com.example.features.LogPriority
import com.example.features.currentConfig
import com.example.features.log
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

suspend fun PipelineContext<Unit, ApplicationCall>.noJwtToken() {
    call.respond(HttpStatusCode.BadRequest, "no [jwtToken] parameter found")
    log("no [jwtToken] parameter found", LogPriority.Debug)
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.jwtTokenIsNotValid() {
    call.respond(HttpStatusCode.Forbidden, "[jwtToken] parameter is not valid")
    log("[jwtToken] parameter is not valid", LogPriority.Debug)
}

suspend fun PipelineContext<Unit, ApplicationCall>.noLogin() {
    call.respond(HttpStatusCode.BadRequest, "no [login] parameter found")
    log("no [login] parameter found", LogPriority.Debug)
}

suspend fun PipelineContext<Unit, ApplicationCall>.noValidLogin() {
    call.respond(HttpStatusCode.BadRequest, "no [login] parameter found")
    log("no [login] parameter found", LogPriority.Debug)
}

suspend fun PipelineContext<Unit, ApplicationCall>.noPassword() {
    call.respond(HttpStatusCode.BadRequest, "no [password] parameter found")
    log("no [password] parameter found", LogPriority.Debug)
}

suspend fun PipelineContext<Unit, ApplicationCall>.noUserId() {
    call.respond(HttpStatusCode.BadRequest, "no [id] parameter found")
    log("no [id] parameter found", LogPriority.Debug)
}

suspend fun PipelineContext<Unit, ApplicationCall>.userIdIsNotLong() {
    call.respond(HttpStatusCode.Forbidden, "[id] parameter is not a long")
    log("[id] parameter is not a long", LogPriority.Debug)
}

suspend fun PipelineContext<Unit, ApplicationCall>.userIdIsNotValid() {
    call.respond(HttpStatusCode.Forbidden, "[id] parameter is not valid")
    log("[id] parameter is not valid", LogPriority.Debug)
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.imageIsTooLarge() {
    call.respond(
        HttpStatusCode.Forbidden,
        "provided image (byte array) is too large, it can be ${currentConfig.fileConfig.profilePictureMaxSize}x${currentConfig.fileConfig.profilePictureMaxSize} at max"
    )
    log("provided image (byte array) is too large, it can be ${currentConfig.fileConfig.profilePictureMaxSize}x${currentConfig.fileConfig.profilePictureMaxSize} at max", LogPriority.Debug)
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.imageIsNotValid() {
    call.respond(HttpStatusCode.Forbidden, "provided image (byte array) is not valid")
    log("provided image (byte array) is not valid", LogPriority.Debug)
}

suspend inline fun PipelineContext<Unit, ApplicationCall>.internalServerError() {
    call.respond(HttpStatusCode.InternalServerError, "Internal server error")
    log("Internal server error", LogPriority.Debug)
}

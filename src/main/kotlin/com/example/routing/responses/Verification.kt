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
package com.example.routing.responses

import com.example.data.local.gamesRepository
import com.example.data.local.usersRepository
import com.example.features.encryption.JwtTokenImpl
import com.example.features.logging.log
import com.example.routing.responses.get.*
import com.example.routing.responses.ws.*
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import io.ktor.util.pipeline.*
import io.opentelemetry.api.logs.Severity

/**
 * possible responses:
 *
 * [noJwtToken]
 *
 * [jwtTokenIsNotValid]
 *
 * [Nothing]
 */
suspend inline fun PipelineContext<Unit, ApplicationCall>.requireValidJwtToken(lambda: () -> Unit) {
    val jwtToken = call.parameters["jwtToken"]
    if (jwtToken == null) {
        log("jwt token is null", Severity.WARN)
        noJwtToken()
        lambda()
        return
    }
    // no need to check for sql injection, cause Exposed handles it for us
    // https://stackoverflow.com/questions/50180516/kotlin-exposed-how-to-create-prepared-statement-or-avoid-sql-injection
    if (!JwtTokenImpl(jwtToken).verify()) {
        log("jwt token is not valid", Severity.WARN)
        jwtTokenIsNotValid()
        lambda()
        return
    }
}

/**
 * possible responses:
 *
 * [noLogin]
 */
suspend inline fun PipelineContext<Unit, ApplicationCall>.requireLogin(lambda: () -> Unit) {
    val login = call.parameters["login"]
    if (login == null) {
        noLogin()
        lambda()
        return
    }
    if (login.length !in 6..30 && login.any { !it.isLetterOrDigit() } || login.any { it.isWhitespace() }) {
        invalidLogin()
        lambda()
        return
    }
}

/**
 * possible responses:
 *
 * [noLogin]
 *
 * [noValidLogin]
 */
suspend inline fun PipelineContext<Unit, ApplicationCall>.requireValidLogin(lambda: () -> Unit) {
    val login = call.parameters["login"]
    if (login == null) {
        noLogin()
        lambda()
        return
    }
    if (!usersRepository.isLoginPresent(login)) {
        noValidLogin()
        lambda()
        return
    }
}

/**
 * possible responses:
 *
 * [noPassword]
 *
 * [Nothing]
 */
suspend inline fun PipelineContext<Unit, ApplicationCall>.requirePassword(lambda: () -> Unit) {
    val password = call.parameters["password"]
    if (password == null) {
        noPassword()
        lambda()
        return
    }
    if (password.length !in 6..30 && password.any { !it.isLetterOrDigit() } || password.any { it.isWhitespace() }) {
        invalidPassword()
        lambda()
        return
    }
}

/**
 * possible responses:
 *
 * [noUserId]
 *
 * [userIdIsNotLong]
 *
 * [userIdIsNotValid]
 *
 * [Nothing]
 */
suspend inline fun PipelineContext<Unit, ApplicationCall>.requireValidUserId(lambda: () -> Unit) {
    val id = call.parameters["id"]
    if (id == null) {
        noUserId()
        lambda()
        return
    }
    if (id.toLongOrNull() == null) {
        userIdIsNotLong()
        lambda()
        return
    }
    if (usersRepository.getLoginById(id.toLong()) == null) {
        userIdIsNotValid()
        lambda()
        return
    }
}

/**
 * possible responses:
 *
 * [noJwtToken]
 *
 * [jwtTokenIsNotValid]
 *
 * [Nothing]
 */
suspend inline fun DefaultWebSocketServerSession.requireValidJwtToken(lambda: () -> Unit) {
    val jwtToken = call.parameters["jwtToken"]
    if (jwtToken == null) {
        noJwtToken()
        lambda()
        return
    }
    if (!JwtTokenImpl(jwtToken).verify()) {
        jwtTokenIsNotValid()
        lambda()
        return
    }
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
suspend inline fun DefaultWebSocketServerSession.requireGameId(lambda: () -> Unit) {
    val gameId = call.parameters["gameId"]
    if (gameId == null) {
        log("no game id parameter found $gameId", Severity.WARN)
        noGameId()
        lambda()
        return
    }
    if (gameId.toLongOrNull() == null) {
        log("game id parameter is not a long $gameId", Severity.WARN)
        gameIdIsNotLong()
        lambda()
        return
    }
    val gameExists = gamesRepository.exists(gameId.toLong())
    if (!gameExists) {
        log("game id parameter is not valid $gameId", Severity.WARN)
        gameIdIsNotValid()
        lambda()
        return
    }
}

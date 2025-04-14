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
package io.github.kroune.routing.responses

import io.github.kroune.data.local.gamesRepository
import io.github.kroune.data.local.usersRepository
import io.github.kroune.features.encryption.JwtTokenImpl
import io.github.kroune.routing.responses.get.*
import io.github.kroune.routing.responses.ws.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

/**
 * possible responses:
 *
 * [noJwtToken]
 *
 * [jwtTokenIsNotValid]
 *
 * [Nothing]
 */
suspend inline fun RoutingContext.requireValidJwtToken(lambda: () -> Unit) {
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
 * [noLogin]
 */
suspend inline fun RoutingContext.requireLogin(lambda: () -> Unit) {
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
suspend inline fun RoutingContext.requireValidLogin(lambda: () -> Unit) {
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
suspend inline fun RoutingContext.requirePassword(lambda: () -> Unit) {
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
suspend inline fun RoutingContext.requireValidUserId(lambda: () -> Unit) {
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
suspend inline fun WebSocketServerSession.requireValidJwtToken(): String? {
    val jwtToken = call.parameters["jwtToken"]
    if (jwtToken == null) {
        noJwtToken()
        return null
    }
    if (!JwtTokenImpl(jwtToken).verify()) {
        jwtTokenIsNotValid()
        return null
    }
    return jwtToken
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
    val gameExists = gamesRepository.exists(gameId.toLong())
    if (!gameExists) {
        gameIdIsNotValid()
        lambda()
        return
    }
}

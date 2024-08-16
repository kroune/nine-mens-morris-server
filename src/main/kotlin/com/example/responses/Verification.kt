package com.example.responses

import com.example.LogPriority
import com.example.data.gamesRepository
import com.example.data.usersRepository
import com.example.encryption.JwtTokenImpl
import com.example.log
import com.example.responses.get.jwtTokenIsNotValid
import com.example.responses.get.noJwtToken
import com.example.responses.get.noLogin
import com.example.responses.get.noPassword
import com.example.responses.get.noUserId
import com.example.responses.get.noValidLogin
import com.example.responses.get.userIdIsNotLong
import com.example.responses.get.userIdIsNotValid
import com.example.responses.ws.gameIdIsNotLong
import com.example.responses.ws.gameIdIsNotValid
import com.example.responses.ws.jwtTokenIsNotValid
import com.example.responses.ws.noGameId
import com.example.responses.ws.noJwtToken
import io.ktor.server.application.*
import io.ktor.server.application.call
import io.ktor.server.websocket.*
import io.ktor.util.pipeline.*
import kotlin.text.toLong
import kotlin.text.toLongOrNull

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
        log("jwt token is null")
        noJwtToken()
        lambda()
        return
    }
    if (!JwtTokenImpl(jwtToken).verify()) {
        log("jwt token is not valid")
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
        log("no game id parameter found $gameId", LogPriority.Debug)
        noGameId()
        lambda()
        return
    }
    if (gameId.toLongOrNull() == null) {
        log("game id parameter is not a long $gameId", LogPriority.Debug)
        gameIdIsNotLong()
        lambda()
        return
    }
    val gameExists = gamesRepository.exists(gameId.toLong())
    if (!gameExists) {
        log("game id parameter is not valid $gameId", LogPriority.Debug)
        gameIdIsNotValid()
        lambda()
        return
    }
}

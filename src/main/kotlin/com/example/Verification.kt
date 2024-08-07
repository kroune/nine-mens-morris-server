package com.example

import com.example.game.Games
import com.example.responses.get.*
import com.example.responses.ws.*
import com.example.users.Users
import com.example.users.Users.validateJwtToken
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import io.ktor.util.pipeline.*

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
        noJwtToken()
        lambda()
        return
    }
    if (!validateJwtToken(jwtToken)) {
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
    if (!Users.isLoginPresent(login)) {
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
    if (Users.getLoginById(id.toLong()).isFailure) {
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
    if (!validateJwtToken(jwtToken)) {
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
    val game = Games.getGame(gameId.toLong())
    if (game == null) {
        log("game id parameter is not valid $gameId", LogPriority.Debug)
        gameIdIsNotValid()
        lambda()
        return
    }
}

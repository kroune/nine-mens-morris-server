package userApi.controller

import common.JwtTokenImpl
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.koin.core.context.GlobalContext
import userApi.data.dao.UsersDataServiceI
import userApi.verify
import userApi.view.*

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
    val koin = GlobalContext.get()
    val usersRepository by koin.inject<UsersDataServiceI>()
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
    val koin = GlobalContext.get()
    val usersRepository by koin.inject<UsersDataServiceI>()
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
package com.example.routing.auth.get

import com.example.data.users.UserData
import com.example.data.usersRepository
import com.example.encryption.JwtTokenImpl
import com.example.json
import com.example.responses.requireLogin
import com.example.responses.requirePassword
import com.example.responses.requireValidJwtToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString

fun Route.accountRoutingGET() {
    /**
     * possible responses:
     *
     * [noLogin]
     *
     * [noPassword]
     *
     * [HttpStatusCode.Conflict] - login is already in use
     *
     * [String] - jwt token
     */
    get("reg") {
        requireLogin {
            return@get
        }
        requirePassword {
            return@get
        }

        val login = call.parameters["login"]!!
        val password = call.parameters["password"]!!
        if (usersRepository.isLoginPresent(login)) {
            call.respond(HttpStatusCode.Conflict, "login is already in use")
            return@get
        }
        val data = UserData(login, password)
        usersRepository.create(data)
        val jwtToken = JwtTokenImpl(login, password).token
        val jsonText = json.encodeToString<String>(jwtToken)
        call.respondText(jsonText)
    }
    /**
     * possible responses:
     *
     * [noLogin]
     *
     * [noPassword]
     *
     * [HttpStatusCode.Unauthorized] - login + password aren't present in the db
     *
     */
    get("login") {
        requireLogin {
            return@get
        }
        requirePassword {
            return@get
        }

        val login = call.parameters["login"]!!
        val password = call.parameters["password"]!!
        val jwtToken = JwtTokenImpl(login, password)
        if (!jwtToken.verify()) {
            call.respond(HttpStatusCode.Unauthorized, "login + password aren't present in the db")
            return@get
        }
        val jsonText = json.encodeToString<String>(jwtToken.token)
        call.respondText(jsonText)
    }
    /**
     * possible responses:
     *
     * [noJwtToken]
     *
     * [jwtTokenIsNotValid]
     *
     * [Boolean] - true
     */
    get("check-jwt-token") {
        requireValidJwtToken {
            return@get
        }

        val jsonText = json.encodeToString<Boolean>(true)
        call.respondText(jsonText)
    }
}

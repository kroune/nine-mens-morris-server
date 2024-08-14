package com.example.routing.auth.get

import com.example.encryption.CustomJwtToken
import com.example.json
import com.example.data.users.UserData
import com.example.data.usersRepository
import com.example.requireLogin
import com.example.requirePassword
import com.example.requireValidJwtToken
import com.example.responses.get.jwtTokenIsNotValid
import com.example.responses.get.noJwtToken
import com.example.responses.get.noLogin
import com.example.responses.get.noPassword
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val data = UserData(login, password, currentDate)
        usersRepository.create(data)
        val jwtToken = CustomJwtToken(login, password).token
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
        val jwtToken = CustomJwtToken(login, password)
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

package com.example.routing.auth.get

import com.example.json
import com.example.requireLogin
import com.example.requirePassword
import com.example.requireValidJwtToken
import com.example.responses.get.jwtTokenIsNotValid
import com.example.responses.get.noJwtToken
import com.example.responses.get.noLogin
import com.example.responses.get.noPassword
import com.example.users.Users.isLoginPresent
import com.example.users.Users.login
import com.example.users.Users.register
import com.example.users.Users.validateLoginData
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
     * [HttpStatusCode.InternalServerError]
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
        if (isLoginPresent(login)) {
            call.respond(HttpStatusCode.Conflict, "login is already in use")
            return@get
        }
        register(login, password).getOrElse {
            call.respond(HttpStatusCode.InternalServerError)
            return@get
        }
        val jwtToken = login(login, password).getOrElse {
            call.respond(HttpStatusCode.InternalServerError)
            return@get
        }
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
     * [HttpStatusCode.InternalServerError]
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
        if (!validateLoginData(login, password)) {
            call.respond(HttpStatusCode.Unauthorized, "login + password aren't present in the db")
            return@get
        }
        val jwtToken = login(login, password).getOrElse {
            call.respond(HttpStatusCode.InternalServerError)
            return@get
        }
        val jsonText = json.encodeToString<String>(jwtToken)
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

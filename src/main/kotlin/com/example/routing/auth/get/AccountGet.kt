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
package com.example.routing.auth.get

import com.example.data.local.users.UserData
import com.example.data.local.usersRepository
import com.example.features.encryption.JwtTokenImpl
import com.example.common.json
import com.example.routing.responses.get.jwtTokenIsNotValid
import com.example.routing.responses.get.noJwtToken
import com.example.routing.responses.get.noLogin
import com.example.routing.responses.get.noPassword
import com.example.routing.responses.requireLogin
import com.example.routing.responses.requirePassword
import com.example.routing.responses.requireValidJwtToken
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
     * [String] - jwt token
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

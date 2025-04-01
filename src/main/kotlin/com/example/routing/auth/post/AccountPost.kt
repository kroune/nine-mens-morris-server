package com.example.routing.auth.post

import com.example.common.json
import com.example.data.local.users.InsertUserData
import com.example.data.local.usersRepository
import com.example.features.encryption.JwtTokenImpl
import com.example.routing.responses.requireLogin
import com.example.routing.responses.requirePassword
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.encodeToString

fun Route.accountRoutingPOST() {
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
    post("reg") {
        requireLogin {
            return@post
        }
        requirePassword {
            return@post
        }

        val login = call.parameters["login"]!!
        val password = call.parameters["password"]!!
        if (usersRepository.isLoginPresent(login)) {
            call.respond(HttpStatusCode.Conflict, "login is already in use")
            return@post
        }
        val data = InsertUserData(login, password)
        usersRepository.create(data)
        val jwtToken = JwtTokenImpl(login, password).token
        val jsonText = json.encodeToString<String>(jwtToken)
        call.respondText(jsonText)
    }
}
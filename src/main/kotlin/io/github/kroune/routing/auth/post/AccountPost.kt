package io.github.kroune.routing.auth.post

import io.github.kroune.common.json
import io.github.kroune.data.local.users.InsertUserData
import io.github.kroune.data.local.usersRepository
import io.github.kroune.features.encryption.JwtTokenImpl
import io.github.kroune.routing.responses.requireLogin
import io.github.kroune.routing.responses.requirePassword
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
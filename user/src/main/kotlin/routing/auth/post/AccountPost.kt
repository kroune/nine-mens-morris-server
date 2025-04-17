package io.github.kroune.routing.auth.post

import JwtTokenImpl
import controller.Bcrypter
import data.InsertUserData
import data.dao.UsersDataServiceI
import io.github.kroune.routing.requireLogin
import io.github.kroune.routing.requirePassword
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Route.accountRoutingPOST() {
    val usersRepository by inject<UsersDataServiceI>()
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
        val passwordHash = Bcrypter.hash(password)
        val data = InsertUserData(login = login, passwordHash = passwordHash)
        usersRepository.create(data)
        val jwtToken = JwtTokenImpl(login, password).token
        val jsonText = Json.encodeToString<String>(jwtToken)
        call.respondText(jsonText)
    }
}
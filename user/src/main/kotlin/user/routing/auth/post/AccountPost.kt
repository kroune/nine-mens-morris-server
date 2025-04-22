package user.routing.auth.post

import user.data.InsertUserPayload
import user.controller.UsersController
import user.routing.requireLogin
import user.routing.requirePassword
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

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
        val usersController by inject<UsersController>()
        val payload = InsertUserPayload(login, password)
        val (statusCode, jwtToken) = usersController.register(payload)
        if (!statusCode.isSuccess()) {
            call.respond(statusCode)
            return@post
        }
        val jsonText = Json.encodeToString<String?>(jwtToken)
        call.respondText(jsonText, status = statusCode)
    }
}
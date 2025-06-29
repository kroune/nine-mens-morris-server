package user.controller.auth.post

import common.logging.logger
import user.model.InsertUserPayload
import user.domain.UsersService
import user.controller.requireLogin
import user.controller.requirePassword
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import user.view.internalServerError

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
        val usersService by inject<UsersService>()
        val payload = InsertUserPayload(login, password)
        val registrationResult = usersService.register(payload)
        when (registrationResult) {
            is UsersService.RegistrationResult.InternalServerError -> {
                logger.error(registrationResult.throwable) { "Internal Server Error at ${call.route}" }
                internalServerError()
            }
            UsersService.RegistrationResult.LoginAlreadyTaken -> {
                call.respond(HttpStatusCode.Conflict, "login already taken")
            }
            is UsersService.RegistrationResult.Success -> {
                call.respond(HttpStatusCode.OK, registrationResult.jwtToken)
            }
        }
    }
}
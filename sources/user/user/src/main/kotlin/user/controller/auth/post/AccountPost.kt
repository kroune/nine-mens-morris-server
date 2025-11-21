package user.controller.auth.post

import common.logging.logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject
import userApi.controller.requireLogin
import userApi.controller.requirePassword
import userApi.domain.UsersServiceI
import userApi.model.InsertUserPayload
import userApi.view.internalServerError

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
        val usersService by inject<UsersServiceI>()
        val payload = InsertUserPayload(login, password)
        val registrationResult = usersService.register(payload)
        when (registrationResult) {
            is UsersServiceI.RegistrationResult.InternalServerError -> {
                logger.error(registrationResult.throwable) { "Internal Server Error at ${call.route}" }
                internalServerError()
            }

            UsersServiceI.RegistrationResult.LoginAlreadyTaken -> {
                call.respond(HttpStatusCode.Conflict, "login already taken")
            }

            is UsersServiceI.RegistrationResult.Success -> {
                call.respond(HttpStatusCode.OK, registrationResult.jwtToken)
            }
        }
    }
}
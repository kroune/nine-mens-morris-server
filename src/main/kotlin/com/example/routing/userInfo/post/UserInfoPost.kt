package com.example.routing.userInfo.post

import com.example.encryption.CustomJwtToken
import com.example.data.usersRepository
import com.example.requireValidJwtToken
import com.example.responses.get.imageIsNotValid
import com.example.responses.get.internalServerError
import com.example.responses.get.jwtTokenIsNotValid
import com.example.responses.get.noJwtToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO

fun Route.userInfoRoutingPOST() {
    /**
     * possible responses:
     *
     * [noJwtToken]
     *
     * [jwtTokenIsNotValid]
     *
     * [HttpStatusCode.InternalServerError]
     *
     * [imageIsNotValid]
     *
     * [Nothing] - success
     */
    post("upload-picture") {
        requireValidJwtToken {
            return@post
        }

        val jwtToken = call.parameters["jwtToken"]!!
        val jwtTokenObject = CustomJwtToken(jwtToken)
        val login = jwtTokenObject.getLogin().getOrElse {
            internalServerError()
            return@post
        }
        val byteArray = try {
            call.receive<ByteArray>()
        } catch (e: ContentTransformationException) {
            imageIsNotValid()
            return@post
        }
        val bytes = ByteArrayInputStream(byteArray)
        try {
            ImageIO.read(bytes)
        } catch (_: IOException) {
            imageIsNotValid()
            return@post
        }
        usersRepository.updatePictureByLogin(login, byteArray)
    }
}

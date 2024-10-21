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
package com.example.routing.userInfo.post

import com.example.features.currentConfig
import com.example.data.local.usersRepository
import com.example.features.encryption.JwtTokenImpl
import com.example.features.logging.log
import com.example.routing.responses.get.*
import com.example.routing.responses.requireValidJwtToken
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.opentelemetry.api.logs.Severity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
     * [imageIsTooLarge]
     *
     * [Nothing] - success
     */
    post("upload-picture") {
        requireValidJwtToken {
            return@post
        }

        val jwtToken = call.parameters["jwtToken"]!!
        val jwtTokenObject = JwtTokenImpl(jwtToken)
        log("getting from jwt token object ${jwtTokenObject.token}", Severity.DEBUG)
        val login = jwtTokenObject.getLogin().getOrElse {
            internalServerError()
            return@post
        }
        log("receiving picture byte array", Severity.DEBUG)
        val byteArray = try {
            call.receive<ByteArray>()
        } catch (_: ContentTransformationException) {
            // actually anything can be converted to byte array
            imageIsNotValid()
            return@post
        }
        log("starting image decoding", Severity.DEBUG)
        val decodedVariant = try {
            val outputStream = ByteArrayOutputStream()

            val bytes = ByteArrayInputStream(byteArray)
            val buffer = ImageIO.read(bytes)!!
            val maxSize = currentConfig.fileConfig.profilePictureMaxSize
            if (buffer.height > maxSize || buffer.width > maxSize) {
                imageIsTooLarge()
                return@post
            }
            ImageIO.write(buffer, "png", outputStream)
            outputStream.close()
            outputStream.toByteArray()
        } catch (_: IOException) {
            imageIsNotValid()
            return@post
        }
        usersRepository.updatePictureByLogin(login, decodedVariant)
        call.respond(HttpStatusCode.OK)
    }
}

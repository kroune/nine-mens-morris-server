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

import com.example.currentConfig
import com.example.data.usersRepository
import com.example.encryption.JwtTokenImpl
import com.example.responses.get.imageIsNotValid
import com.example.responses.get.imageIsTooLarge
import com.example.responses.get.internalServerError
import com.example.responses.requireValidJwtToken
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
        val decodedVariant = try {
            val bytes = ByteArrayInputStream(byteArray)
            ImageIO.read(bytes)!!
        } catch (_: IOException) {
            imageIsNotValid()
            return@post
        }
        val maxSize = currentConfig.fileConfig.profilePictureMaxSize
        if (decodedVariant.height > maxSize || decodedVariant.width > maxSize) {
            imageIsTooLarge()
            return@post
        }
        usersRepository.updatePictureByLogin(login, byteArray)
    }
}

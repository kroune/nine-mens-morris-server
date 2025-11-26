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
package user.controller.userinfo.post

import common.ConfigurationLoader.currentConfig
import common.JwtTokenImpl
import common.logging.globalLogger
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject
import userApi.controller.requireValidJwtToken
import userApi.data.dao.UsersDataServiceI
import userApi.view.imageIsNotValid
import userApi.view.imageIsTooLarge
import userApi.view.internalServerError
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
    rateLimit(RateLimitName("imageUploading")) {
        post("upload-picture") {
            requireValidJwtToken {
                return@post
            }

            val jwtToken = call.parameters["jwtToken"]!!
            val jwtTokenObject = JwtTokenImpl(jwtToken)
            globalLogger.atDebug {
                message = "getting from jwt token object ${jwtTokenObject.token}"
            }
            val login = jwtTokenObject.getLogin().getOrElse {
                internalServerError()
                return@post
            }
            globalLogger.atDebug {
                message = "receiving picture byte array"
            }
            val byteArray = try {
                call.receive<ByteArray>()
            } catch (_: ContentTransformationException) {
                // actually anything can be converted to byte array
                imageIsNotValid()
                return@post
            }
            globalLogger.atDebug {
                message = "starting image decoding"
            }
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
            } catch (e: Exception) {
                when (e) {
                    is IOException, is IllegalArgumentException, is NullPointerException -> {
                        imageIsNotValid()
                        return@post
                    }

                    else -> {
                        internalServerError()
                        globalLogger.atError {
                            message = "unrecognized exception when decoding image"
                        }
                        return@post
                    }
                }
            }
            val usersRepository by inject<UsersDataServiceI>()
            usersRepository.updatePictureByLogin(login, decodedVariant)
            call.respond(HttpStatusCode.OK)
        }
    }
}

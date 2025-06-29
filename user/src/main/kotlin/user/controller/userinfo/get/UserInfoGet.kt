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
package user.controller.userinfo.get

import common.logging.globalLogger
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import user.controller.requireValidJwtToken
import user.controller.requireValidUserId
import user.data.dao.UsersDataServiceI
import user.view.internalServerError

/**
 * Tests - [UserInfoGetTest]
 */
fun Route.userInfoRoutingGET() {
    /**
     * possible responses:
     *
     * [noUserId]
     *
     * [userIdIsNotLong]
     *
     * [userIdIsNotValid]
     *
     * [internalServerError]
     *
     * [String] - login
     */
    get("get-login-by-id") {
        requireValidUserId {
            return@get
        }
        requireValidJwtToken {
            return@get
        }

        val id = call.parameters["id"]!!.toLong()
        val usersRepository by inject<UsersDataServiceI>()
        val text = usersRepository.getLoginById(id) ?: run {
            globalLogger.atError {
                message = "id was marked as valid, but getting login from db failed"
            }
            internalServerError()
            return@get
        }
        call.respond(text)
    }
    /**
     * possible responses:
     *
     * [noUserId]
     *
     * [userIdIsNotLong]
     *
     * [userIdIsNotValid]
     *
     * [internalServerError]
     *
     * [Int] - rating
     */
    get("get-creation-date-by-id") {
        requireValidUserId {
            return@get
        }
        requireValidJwtToken {
            return@get
        }

        val id = call.parameters["id"]!!.toLong()
        val usersRepository by inject<UsersDataServiceI>()
        val text = (usersRepository.getCreationDateById(id) ?: run {
            globalLogger.atError {
                message = "id was marked as valid, but getting creation date from db failed"
            }
            internalServerError()
            return@get
        }).let {
            Triple(it.dayOfMonth, it.monthNumber, it.year)
        }
        call.respond<Triple<Int, Int, Int>>(text)
    }
    /**
     * possible responses:
     *
     * [noUserId]
     *
     * [userIdIsNotLong]
     *
     * [userIdIsNotValid]
     *
     * [internalServerError]
     *
     * [Int] - rating
     */
    get("get-rating-by-id") {
        requireValidUserId {
            return@get
        }
        requireValidJwtToken {
            return@get
        }

        val id = call.parameters["id"]!!.toLong()
        val usersRepository by inject<UsersDataServiceI>()
        val text = usersRepository.getRatingById(id) ?: run {
            globalLogger.atError {
                message = "id was marked as valid, but getting rating from db failed"
            }
            internalServerError()
            return@get
        }
        call.respond<Int>(text)
    }
    /**
     * possible responses:
     *
     * [noUserId]
     *
     * [userIdIsNotLong]
     *
     * [userIdIsNotValid]
     *
     * [ByteArray] - profile picture
     */
    get("get-picture-by-id") {
        requireValidUserId {
            return@get
        }
        val id = call.parameters["id"]!!.toLong()

        requireValidJwtToken {
            return@get
        }

        val defaultPicture = this.javaClass.getResource("/default_profile_image.png")?.readBytes() ?: run {
            globalLogger.atError {
                message = "default profile picture is missing"
            }
            internalServerError()
            return@get
        }
        val usersRepository by inject<UsersDataServiceI>()
        val picture = usersRepository.getPictureById(id) ?: defaultPicture
        call.respond(HttpStatusCode.OK, picture)
    }
    /**
     * possible responses:
     *
     * [noJwtToken]
     *
     * [jwtTokenIsNotValid]
     *
     * [internalServerError]
     *
     * [Long] - user id
     */
    get("get-id-by-jwt-token") {
        requireValidJwtToken {
            return@get
        }

        val jwtToken = call.parameters["jwtToken"]!!
        val usersRepository by inject<UsersDataServiceI>()
        val id: Long = usersRepository.getIdByJwtToken(jwtToken) ?: run {
            globalLogger.atError {
                message = "jwt token was marked as valid, but getting id from db failed"
            }
            internalServerError()
            return@get
        }
        call.respond(HttpStatusCode.OK, id)
    }
    get("leaderboard") {
        requireValidJwtToken {
            return@get
        }

        val amount = (call.parameters["amount"] ?: 10.toString()).toIntOrNull()?.coerceIn(0..100)
        if (amount == null) {
            call.respond(HttpStatusCode.BadRequest, "no [amount] parameter found")
            return@get
        }

        val usersRepository by inject<UsersDataServiceI>()
        val leaderboard = usersRepository.getLeaderboard(amount)
        call.respond(HttpStatusCode.OK, leaderboard)
    }
}

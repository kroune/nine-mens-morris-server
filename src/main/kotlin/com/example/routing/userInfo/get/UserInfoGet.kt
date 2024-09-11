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
package com.example.routing.userInfo.get

import com.example.LogPriority
import com.example.data.usersRepository
import com.example.json
import com.example.log
import com.example.responses.get.*
import com.example.responses.requireValidJwtToken
import com.example.responses.requireValidLogin
import com.example.responses.requireValidUserId
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString

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

        val id = call.parameters["id"]!!.toLong()
        val text = usersRepository.getLoginById(id) ?: run {
            log("id was marked as valid, but getting login from db failed", LogPriority.Errors)
            internalServerError()
            return@get
        }
        val jsonText = json.encodeToString<String>(text)
        call.respondText(jsonText)
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

        val id = call.parameters["id"]!!.toLong()
        val text = (usersRepository.getCreationDateById(id) ?: run {
            log("id was marked as valid, but getting creation date from db failed", LogPriority.Errors)
            internalServerError()
            return@get
        }).let {
            Triple(it.dayOfMonth, it.monthNumber, it.year)
        }
        val jsonText = json.encodeToString<Triple<Int, Int, Int>>(text)
        call.respondText(jsonText)
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

        val id = call.parameters["id"]!!.toLong()
        val text = usersRepository.getRatingById(id) ?: run {
            log("id was marked as valid, but getting rating from db failed", LogPriority.Errors)
            internalServerError()
            return@get
        }
        val jsonText = json.encodeToString<Int>(text)
        call.respondText(jsonText)
    }
    /**
     * possible responses:
     *
     * [noLogin]
     *
     * [noValidLogin]
     *
     * [internalServerError]
     *
     * [Long] - profile id
     */
    get("get-id-by-login") {
        requireValidLogin {
            return@get
        }

        val login = call.parameters["login"]!!.toString()
        val id: Long = usersRepository.getIdByLogin(login) ?: run {
            log("login was marked as valid, but getting id from db failed", LogPriority.Errors)
            internalServerError()
            return@get
        }
        val jsonText = json.encodeToString<Long>(id)
        call.respondText(jsonText)
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
        val defaultPicture = this.javaClass.getResource("/default_profile_image.png")?.readBytes() ?: run {
            log("default profile picture is missing", LogPriority.Errors)
            internalServerError()
            return@get
        }
        val picture = usersRepository.getPictureById(id) ?: defaultPicture
        val jsonText = json.encodeToString<ByteArray>(picture)
        call.respondText(jsonText)
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
        val id: Long = usersRepository.getIdByJwtToken(jwtToken) ?: run {
            log("jwt token was marked as valid, but getting id from db failed", LogPriority.Errors)
            internalServerError()
            return@get
        }
        val jsonText = json.encodeToString<Long>(id)
        call.respondText(jsonText)
    }
}

package com.example.routing.userInfo.get

import com.example.json
import com.example.data.usersRepository
import com.example.requireValidJwtToken
import com.example.requireValidLogin
import com.example.requireValidUserId
import com.example.responses.get.*
import com.example.responses.ws.jwtTokenIsNotValid
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import java.io.File

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
     * [HttpStatusCode.InternalServerError]
     *
     * [String] - login
     */
    get("get-login-by-id") {
        requireValidUserId {
            return@get
        }

        val id = call.parameters["id"]!!.toLong()
        val text = usersRepository.getLoginById(id) ?: run {
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
     * [HttpStatusCode.InternalServerError]
     *
     * [Int] - rating
     */
    get("get-creation-date-by-id") {
        requireValidUserId {
            return@get
        }

        val id = call.parameters["id"]!!.toLong()
        val text = (usersRepository.getCreationDateById(id) ?: run {
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
     * [HttpStatusCode.InternalServerError]
     *
     * [Int] - rating
     */
    get("get-rating-by-id") {
        requireValidUserId {
            return@get
        }

        val id = call.parameters["id"]!!.toLong()
        val text = usersRepository.getRatingById(id) ?: run {
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
     * [HttpStatusCode.InternalServerError]
     *
     * [Long] - profile id
     */
    get("get-id-by-login") {
        requireValidLogin {
            return@get
        }

        val login = call.parameters["login"]!!.toString()
        val id: Long = usersRepository.getIdByLogin(login) ?: run {
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
        val defaultPicture = File("default/img.png")
        require(defaultPicture.exists())
        val picture = usersRepository.getPictureById(id) ?: defaultPicture.readBytes()
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
     * [HttpStatusCode.InternalServerError]
     *
     * [Long] - user id
     */
    get("get-id-by-jwt-token") {
        requireValidJwtToken {
            return@get
        }

        val jwtToken = call.parameters["jwtToken"]!!
        val id: Long = usersRepository.getIdByJwtToken(jwtToken) ?: run {
            internalServerError()
            return@get
        }
        val jsonText = json.encodeToString<Long>(id)
        call.respondText(jsonText)
    }
}

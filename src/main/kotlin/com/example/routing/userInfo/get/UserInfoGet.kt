package com.example.routing.userInfo.get

import com.example.json
import com.example.requireValidJwtToken
import com.example.requireValidLogin
import com.example.requireValidUserId
import com.example.responses.get.jwtTokenIsNotValid
import com.example.responses.get.noJwtToken
import com.example.responses.ws.jwtTokenIsNotValid
import com.example.users.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import java.io.File

fun Route.userInfoRoutingGET() {
    get("get-login-by-id") {
        requireValidUserId {
            return@get
        }

        val id = call.parameters["id"]!!.toLong()
        val text = Users.getLoginById(id).getOrThrow()
        val jsonText = json.encodeToString<String?>(text)
        call.respondText(jsonText)
    }
    get("get-creation-date-by-id") {
        requireValidUserId {
            return@get
        }

        val id = call.parameters["id"]!!.toLong()
        val text = Users.getCreationDateById(id).getOrThrow()
        val jsonText = json.encodeToString<Triple<Int, Int, Int>?>(text)
        call.respondText(jsonText)
    }
    get("get-rating-by-id") {
        requireValidUserId {
            return@get
        }

        val id = call.parameters["id"]!!.toLong()
        val text = Users.getRatingById(id).getOrThrow()
        val jsonText = json.encodeToString<Long>(text)
        call.respondText(jsonText)
    }
    get("get-id-by-login") {
        requireValidLogin {
            return@get
        }

        val login = call.parameters["login"]!!.toString()
        val id: Long = Users.getIdByLogin(login).getOrThrow()
        val jsonText = json.encodeToString<Long>(id)
        call.respondText(jsonText)
    }
    get("get-picture-by-id") {
        requireValidUserId {
            return@get
        }

        val id = call.parameters["id"]!!.toLong()
        val defaultPicture = File("default/img.png")
        require(defaultPicture.exists())
        val picture = Users.getPictureById(id).getOrNull() ?: defaultPicture.readBytes()
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
        val id: Long = Users.getIdByJwtToken(jwtToken).getOrElse {
            call.respond(HttpStatusCode.InternalServerError)
            return@get
        }
        val jsonText = json.encodeToString<Long>(id)
        call.respondText(jsonText)
    }
}

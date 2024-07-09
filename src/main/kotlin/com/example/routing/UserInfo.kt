package com.example.routing

import com.example.jwtToken.CustomJwtToken
import com.example.users.Users
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


fun Route.userInfoRouting() {
    get("get-login-by-id") {
        val id = call.parameters["id"]!!.toLong()
        val text = Users.getLoginById(id).getOrNull()
        val jsonText = Json.encodeToString<String?>(text)
        call.respondText { jsonText }
    }
    get("get-creation-date-by-id") {
        val id = call.parameters["id"]!!.toLong()
        val text = Users.getCreationDateById(id).getOrNull()
        val jsonText = Json.encodeToString<Triple<Int, Int, Int>?>(text)
        call.respondText { jsonText }
    }
    get("get-rating-by-id") {
        val id = call.parameters["id"]!!.toLong()
        val text = Users.getRatingById(id).getOrNull()
        val jsonText = Json.encodeToString<Long?>(text)
        call.respondText { jsonText }
    }
    get("get-id-by-login") {
        val login = call.parameters["login"]!!.toString()
        val id = Users.getIdByLogin(login).getOrNull()
        val jsonText = Json.encodeToString<Long?>(id)
        call.respondText { jsonText }
    }
    get("get-picture-by-id") {
        val id = call.parameters["id"]!!.toLong()
        val defaultPicture = File("default/img.png")
        require(defaultPicture.exists())
        val picture = Users.getPictureById(id).getOrDefault(defaultPicture.readBytes())
        val jsonText = Json.encodeToString<ByteArray>(picture)
        call.respondText { jsonText }
    }
    get("get-id-by-jwt-token") {
        val jwtToken = call.parameters["jwtToken"]!!.toString()
        val jwtTokenObject = CustomJwtToken(jwtToken)
        if (!Users.validateJwtToken(jwtTokenObject)) {
            call.respond { Json.encodeToString<Long>(-1L) }
            return@get
        }
        val login = jwtTokenObject.getLogin().getOrElse {
            call.respond { Json.encodeToString<Long>(-1L) }
            return@get
        }
        val id = Users.getIdByLogin(login).getOrElse {
            call.respond { Json.encodeToString<Long>(-1L) }
            return@get
        }
        call.respondText { Json.encodeToString<Long>(id) }
    }
}

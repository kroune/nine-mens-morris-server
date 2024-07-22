package com.example.routing.api

import com.example.CustomJwtToken
import com.example.json
import com.example.users.Users
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO


fun Route.userInfoRouting() {
    get("get-login-by-id") {
        val id = call.parameters["id"]!!.toLong()
        val text = Users.getLoginById(id).getOrNull()
        val jsonText = json.encodeToString<String?>(text)
        call.respondText { jsonText }
    }
    get("get-creation-date-by-id") {
        val id = call.parameters["id"]!!.toLong()
        val text = Users.getCreationDateById(id).getOrNull()
        val jsonText = json.encodeToString<Triple<Int, Int, Int>?>(text)
        call.respondText { jsonText }
    }
    get("get-rating-by-id") {
        val id = call.parameters["id"]!!.toLong()
        val text = Users.getRatingById(id).getOrNull()
        val jsonText = json.encodeToString<Long?>(text)
        call.respondText { jsonText }
    }
    get("get-id-by-login") {
        val login = call.parameters["login"]!!.toString()
        val id = Users.getIdByLogin(login).getOrNull()
        val jsonText = json.encodeToString<Long?>(id)
        call.respondText { jsonText }
    }
    post("upload-picture") {
        val jwtToken = call.parameters["jwtToken"]!!
        val jwtTokenObject = CustomJwtToken(jwtToken)
        if (!Users.validateJwtToken(jwtTokenObject)) {
            return@post
        }
        val id = Users.getIdByJwtToken(jwtTokenObject).getOrThrow()
        val byteArray = call.receive<ByteArray>()
        val bytes = ByteArrayInputStream(byteArray)
        ImageIO.read(bytes)
        Users.uploadPictureById(byteArray, id)
    }
    get("get-picture-by-id") {
        val id = call.parameters["id"]!!.toLong()
        val defaultPicture = File("default/img.png")
        require(defaultPicture.exists())
        val picture = Users.getPictureById(id).getOrDefault(defaultPicture.readBytes())
        val jsonText = json.encodeToString<ByteArray>(picture)
        call.respondText { jsonText }
    }
    get("get-id-by-jwt-token") {
        val jwtToken = call.parameters["jwtToken"]!!
        val jwtTokenObject = CustomJwtToken(jwtToken)
        if (!Users.validateJwtToken(jwtTokenObject)) {
            call.respond { json.encodeToString<Long>(-1L) }
            return@get
        }
        val login = jwtTokenObject.getLogin().getOrElse {
            call.respond { json.encodeToString<Long>(-1L) }
            return@get
        }
        val id = Users.getIdByLogin(login).getOrElse {
            call.respond { json.encodeToString<Long>(-1L) }
            return@get
        }
        call.respondText { json.encodeToString<Long>(id) }
    }
}

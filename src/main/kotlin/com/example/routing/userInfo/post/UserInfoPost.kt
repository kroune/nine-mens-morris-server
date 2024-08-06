package com.example.routing.userInfo.post

import com.example.responses.get.imageIsNotValid
import com.example.requireValidJwtToken
import com.example.users.Users
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO

fun Route.userInfoRoutingPOST() {
    post("upload-picture") {
        requireValidJwtToken {
            return@post
        }

        val jwtToken = call.parameters["jwtToken"]!!
        val id = Users.getIdByJwtToken(jwtToken).getOrThrow()
        val byteArray = call.receive<ByteArray>()
        val bytes = ByteArrayInputStream(byteArray)
        try {
            ImageIO.read(bytes)
        } catch (_: IOException) {
            com.example.responses.get.imageIsNotValid()
            return@post
        }
        Users.uploadPictureById(byteArray, id)
    }
}
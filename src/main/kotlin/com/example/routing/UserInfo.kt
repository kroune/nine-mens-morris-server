package com.example.routing

import com.example.users.Users
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


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
        call.respondText { jsonText}
    }
}

package com.example.routing

import com.example.users.Users
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.userInfoRouting() {
    get("get-login-by-id") {
        val id = call.parameters["id"]!!.toLong()
        call.respondText { Users.getLoginById(id).getOrNull().toString() }
    }
    get("get-creation-date-by-id") {
        val id = call.parameters["id"]!!.toLong()
        call.respondText { Users.getCreationDateById(id).getOrNull().toString() }
    }
}
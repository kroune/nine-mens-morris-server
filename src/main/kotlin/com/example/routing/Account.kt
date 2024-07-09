package com.example.routing

import com.example.CustomJwtToken
import com.example.users.Users.checkLoginData
import com.example.users.Users.isLoginPresent
import com.example.users.Users.login
import com.example.users.Users.register
import com.example.users.Users.validateJwtToken
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Route.accountRouting() {
    get("reg") {
        val login = call.parameters["login"]!!
        val password = call.parameters["password"]!!
        if (isLoginPresent(login)) {
            notify(409, "login is already in use")
            return@get
        }
        try {
            register(login, password).getOrThrow()
        } catch (e: IllegalStateException) {
            notify(401, "server error")
            println("error registering")
            e.printStackTrace()
            return@get
        }
        try {
            val jwtToken = login(login, password).getOrThrow()
            notify(200, jwtToken)
        } catch (e: IllegalStateException) {
            notify(401, "server error")
            println("error logging in")
            e.printStackTrace()
            return@get
        }
    }
    get("login") {
        val login = call.parameters["login"]!!
        val password = call.parameters["password"]!!
        try {
            if (!checkLoginData(login, password)) {
                error("cookie isn't present")
            }
            val bdCallResult = login(login, password).getOrThrow()
            notify(200, bdCallResult)
        } catch (e: IllegalStateException) {
            notify(401, "server error")
            println("error logging in")
            e.printStackTrace()
            return@get
        }
    }
    get("check-jwt-token") {
        val jwtToken = CustomJwtToken(call.parameters["jwtToken"]!!)
        notify(200, validateJwtToken(jwtToken).toString())
    }
}

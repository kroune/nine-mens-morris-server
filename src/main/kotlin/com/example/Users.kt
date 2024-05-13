package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.plugins.SECRET
import java.time.Duration
import java.util.*


object Users {
    private val users = Collections.synchronizedList<User>(mutableListOf())
    fun checkJWTToken(jwtToken: String): Boolean {
        return users.any {
            it.jwtToken == jwtToken
        }
    }

    private fun isLoginPresent(login: String): Boolean {
        return users.any {
            it.login == login
        }
    }

    private fun checkLoginData(login: String, password: String): Boolean {
        return users.any {
            JWT.decode(it.jwtToken).claims["login"]?.asString() == login &&
                    JWT.decode(it.jwtToken).claims["password"]?.asString() == password
        }
    }

    fun register(login: String, password: String) {
        if (isLoginPresent(login)) {
            error("user with the same login already exists")
        }
        val cookie = createJWTToken(login, password)
        users.add(User(login, cookie))
    }

    fun login(login: String, password: String): String {
        if (!checkLoginData(login, password)) {
            error("cookie isn't present")
        }
        val token = createJWTToken(login, password)
        val test = JWT.decode(token)
        println(test)
        return token
    }

    private fun createJWTToken(login: String, password: String): String {
        val token = JWT.create()
            .withClaim("login", login)
            .withClaim("password", password)
            .withExpiresAt(Date(System.currentTimeMillis() + Duration.ofHours(1L).toMillis()))
            .sign(Algorithm.HMAC256(SECRET))
        return token
    }
}

class User(val login: String, val jwtToken: String)
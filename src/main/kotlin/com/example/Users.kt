package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.plugins.SECRET
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.util.*


object Users {
    private val users = Collections.synchronizedList<User>(mutableListOf())

    init {
        val file = File("data")
        val data = file.readText()
        users.addAll(Json.decodeFromString<List<User>>(data))
    }

    fun checkJWTToken(jwtToken: String): Boolean {
        // we CAN'T directly compare jwt tokens
        return users.any {
            JWT.decode(it.jwtToken).claims["login"]?.asString() == JWT.decode(jwtToken).claims["login"]?.asString() &&
                    JWT.decode(it.jwtToken).claims["password"]?.asString() == JWT.decode(jwtToken).claims["password"]?.asString()
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
        store()
    }

    private fun store() {
        // this is top level security
        val file = File("data")
        val text = Json.encodeToString(users)
        file.writeText(text)
    }

    fun login(login: String, password: String): String {
        if (!checkLoginData(login, password)) {
            error("cookie isn't present")
        }
        val token = createJWTToken(login, password)
        println(token)
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

@Serializable
data class User(@Serializable val login: String, @Serializable val jwtToken: String)
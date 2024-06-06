package com.example

import com.example.jwtToken.CustomJwtToken
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*


object Users {
    private val users = Collections.synchronizedList<User>(mutableListOf())
    private val loginsMap = mutableMapOf<String, CustomJwtToken>()

    init {
        val file = File("data")
        if (!file.exists()) {
            file.createNewFile()
            file.writeText("[]")
        }
        val data = file.readText()
        Json.decodeFromString<List<User>>(data).forEach {
            users.add(it)
            loginsMap[it.login] = it.jwtToken
        }
    }

    private fun checkLoginData(login: String, password: String): Boolean {
        val jwtToken = CustomJwtToken(login, password)
        return validateJwtToken(jwtToken)
    }

    fun validateJwtToken(jwtToken: CustomJwtToken): Boolean {
        val login = jwtToken.getLogin().getOrNull() ?: return false
        return loginsMap[login] == jwtToken
    }

    private fun isLoginPresent(login: String): Boolean {
        return loginsMap[login] != null
    }

    fun register(login: String, password: String) {
        if (isLoginPresent(login)) {
            error("user with the same login already exists")
        }
        val jwtToken = CustomJwtToken(login, password)
        users.add(User(login, jwtToken))
        loginsMap[login] = jwtToken
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
        val token = CustomJwtToken(login, password).token
        println(token)
        return token
    }
}

@Serializable
data class User(
    @Serializable val login: String,
    @Serializable val jwtToken: CustomJwtToken
)
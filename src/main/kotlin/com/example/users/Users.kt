package com.example.users

import com.example.jwtToken.CustomJwtToken
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.util.*
import java.util.concurrent.atomic.AtomicLong


object Users {
    private val users = Collections.synchronizedList<User>(mutableListOf())
    private val loginsToIdMap = mutableMapOf<String, Long>()
    private val idToUsersMap = mutableMapOf<Long, User>()
    val idCounter = AtomicLong(1)

    init {
        val file = File("data")
        if (!file.exists()) {
            file.createNewFile()
            file.writeText("[]")
        }
        val data = file.readText()
        Json.decodeFromString<List<User>>(data).forEach {
            users.add(it)
            idToUsersMap[it.id] = it
            loginsToIdMap[it.login] = it.id
        }
    }

    fun getIdByLogin(login: String): Result<Long> {
        return runCatching {
            loginsToIdMap[login]!!
        }
    }

    fun getLoginById(id: Long): Result<String> {
        return runCatching {
            getUserById(id).getOrThrow().login
        }
    }

    fun getCreationDateById(id: Long): Result<Calendar> {
        return runCatching {
            getUserById(id).getOrThrow().calendar
        }
    }

    private fun getUserById(id: Long): Result<User> {
        return runCatching {
            idToUsersMap[id]!!
        }
    }

    private fun getUserByLogin(login: String): Result<User> {
        return runCatching {
            getUserById(getIdByLogin(login).getOrThrow()).getOrThrow()
        }
    }

    fun checkLoginData(login: String, password: String): Boolean {
        val jwtToken = CustomJwtToken(login, password)
        return validateJwtToken(jwtToken)
    }

    fun validateJwtToken(jwtToken: CustomJwtToken): Boolean {
        val login = jwtToken.getLogin().getOrNull() ?: return false
        return getUserByLogin(login).getOrNull()?.jwtToken == jwtToken
    }

    fun isLoginPresent(login: String): Boolean {
        return getIdByLogin(login).isSuccess
    }

    fun register(login: String, password: String): Result<Unit> {
        return runCatching {
            if (isLoginPresent(login)) {
                error("user with the same login already exists")
            }
            val jwtToken = CustomJwtToken(login, password)
            val calendar = Calendar.getInstance()
            LocalDate.now().let {
                calendar.set(it.dayOfYear, it.monthValue, it.year)
            }
            val id = idCounter.incrementAndGet()
            val user = User(login, id, calendar, jwtToken)
            users.add(user)
            loginsToIdMap[login] = id
            idToUsersMap[id] = user
            store()
        }
    }

    private fun store() {
        // this is top level security
        val file = File("data")
        val text = Json.encodeToString(users)
        file.writeText(text)
    }

    fun login(login: String, password: String): Result<String> {
        return runCatching {
            if (!checkLoginData(login, password)) {
                error("cookie isn't present")
            }
            CustomJwtToken(login, password).token
        }
    }
}

@Serializable
data class User(
    @Serializable val login: String,
    @Serializable val id: Long,
    @Contextual @Serializable val calendar: Calendar,
    @Serializable val jwtToken: CustomJwtToken
)
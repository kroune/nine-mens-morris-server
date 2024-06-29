package com.example.users

import com.example.jwtToken.CustomJwtToken
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
    val idCounter = AtomicLong(0)

    init {
        run {
            val file = File("data/users")
            if (!file.exists()) {
                file.createNewFile()
                file.writeText("[]")
            }
            val data = file.readText()
            Json.decodeFromString<List<User>>(data).forEach {
                println("new info ${it.login} ${it.id} ${it.date} ${it.jwtToken}")
                users.add(it)
                idToUsersMap[it. id] = it
                loginsToIdMap[it.login] = it.id
            }
        }
        run {
            val file = File("data/counter")
            if (!file.exists()) {
                file.createNewFile()
                file.writeText("0")
            }
            val data = file.readText()
            idCounter.set(data.toLong())
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
        }.onFailure {
            println("getting login failed")
            it.printStackTrace()
        }
    }

    fun getCreationDateById(id: Long): Result<Triple<Int, Int, Int>> {
        return runCatching {
            getUserById(id).getOrThrow().date
        }
    }

    private fun getUserById(id: Long): Result<User> {
        return runCatching {
            idToUsersMap[id]!!
        }.onFailure {
            println("getting user by id failed")
            it.printStackTrace()
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
            val date = LocalDate.now().let {
                Triple(it.dayOfYear, it.monthValue, it.year)
            }
            val id = idCounter.incrementAndGet()
            val user = User(login, id, date, jwtToken)
            users.add(user)
            loginsToIdMap[login] = id
            idToUsersMap[id] = user
            store()
        }
    }

    private fun store() {
        run {
            // this is top level security
            val file = File("data/users")
            val text = Json.encodeToString(users)
            file.writeText(text)
        }
        run {
            val file = File("data/counter")
            val text = idCounter.get().toString()
            file.writeText(text)
        }
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
    @Serializable val date: Triple<Int, Int, Int>,
    @Serializable val jwtToken: CustomJwtToken
)

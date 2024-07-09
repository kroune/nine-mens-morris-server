package com.example.users

import com.example.jwtToken.CustomJwtToken
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.util.*
import java.util.concurrent.atomic.AtomicLong

val dataDir = File("data")

object Users {
    private val users = Collections.synchronizedList<User>(mutableListOf())
    private val loginsToIdMap = mutableMapOf<String, Long>()
    private val idToUsersMap = mutableMapOf<Long, User>()
    private val idCounter = AtomicLong(0)

    init {
        dataDir.mkdirs()
        run {
            val usersDir = File(dataDir, "users")
            usersDir.mkdirs()
            usersDir.listFiles()!!.forEach { file ->
                val data = file.readText()
                Json.decodeFromString<User>(data).let {
                    println("new info ${it.login} ${it.id} ${it.date} ${it.jwtToken}")
                    users.add(it)
                    idToUsersMap[it. id] = it
                    loginsToIdMap[it.login] = it.id
                }
            }
        }
        run {
            val file = File(dataDir, "counter")
            if (!file.exists()) {
                file.createNewFile()
                file.writeText("0")
            }
            val data = file.readText()
            idCounter.set(data.toLong())
        }
    }

    private fun store() {
        dataDir.mkdirs()
        run {
            // this is top level security
            val usersDir = File(dataDir, "users")
            users.forEach {
                val userDataFile = File(usersDir, it.login)
                val text = Json.encodeToString(it)
                userDataFile.writeText(text)
            }
        }
        run {
            val file = File(dataDir, "counter")
            val text = idCounter.get().toString()
            file.writeText(text)
        }
    }

    fun getPictureById(id: Long): Result<ByteArray> {
        return runCatching {
            getUserById(id).getOrThrow().profilePicture!!
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

    fun getRatingById(id: Long): Result<Long> {
        return runCatching {
            getUserById(id).getOrThrow().rating
        }
    }

    fun getCreationDateById(id: Long): Result<Triple<Int, Int, Int>> {
        return runCatching {
            getUserById(id).getOrThrow().date
        }
    }

    fun updateRatingById(id: Long, deltaRating: Long) {
        val user = idToUsersMap[id] ?: return
        user.rating = (deltaRating + user.rating).coerceAtLeast(0)
        store()
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

    fun getIdByJwtToken(jwtToken: CustomJwtToken): Result<Long> {
        return runCatching {
            loginsToIdMap[jwtToken.getLogin().getOrThrow()]!!
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
                Triple(it.dayOfMonth, it.monthValue, it.year)
            }
            val id = idCounter.incrementAndGet()
            val user = User(login, id, date, jwtToken, 1000L, byteArrayOf())
            users.add(user)
            loginsToIdMap[login] = id
            idToUsersMap[id] = user
            store()
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
    @Serializable val jwtToken: CustomJwtToken,
    @Serializable var rating: Long,
    @Serializable val profilePicture: ByteArray?
)

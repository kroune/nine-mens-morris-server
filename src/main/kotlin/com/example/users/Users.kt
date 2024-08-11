package com.example.users

import com.auth0.jwt.exceptions.JWTDecodeException
import com.example.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File
import java.time.LocalDate
import java.util.*
import java.util.concurrent.atomic.AtomicLong

val dataDir = File(currentConfig.fileConfig.dataDir)

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
                json.decodeFromString<User>(data).let {
                    println("new info ${it.login} ${it.id} ${it.date} ${it.jwtToken.getLogin()}")
                    users.add(it)
                    idToUsersMap[it.id] = it
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
                val userDataFile = File(usersDir, "${it.login}.json")
                val text = json.encodeToString(it)
                userDataFile.writeText(text)
            }
        }
        run {
            val file = File(dataDir, "counter")
            val text = idCounter.get().toString()
            file.writeText(text)
        }
    }

    fun uploadPictureById(id: Long, newPicture: ByteArray): Result<Unit> {
        return runCatching {
            getUserById(id).getOrThrow().profilePicture = newPicture
        }.onFailure {
            log("uploading picture failed", LogPriority.Errors)
            it.printStackTrace()
        }
    }

    fun uploadPictureByLogin(login: String, newPicture: ByteArray): Result<Unit> {
        return runCatching {
            getUserByLogin(login).getOrThrow().profilePicture = newPicture
            store()
        }.onFailure {
            log("uploading picture failed", LogPriority.Errors)
            it.printStackTrace()
        }
    }

    /**
     * possible results:
     *
     * [NullPointerException] - getting user by id failed | profile picture is null
     *
     * [ByteArray] - image file as a byte array
     */
    fun getPictureById(id: Long): Result<ByteArray?> {
        return runCatching {
            getUserById(id).getOrThrow().profilePicture
        }.onFailure {
            log("getting picture failed", LogPriority.Errors)
            it.printStackTrace()
        }
    }

    /**
     * possible results:
     *
     * [NullPointerException] - no user with such id exists
     *
     * [Long] - id of the user
     */
    fun getIdByLogin(login: String): Result<Long> {
        return runCatching {
            loginsToIdMap[login]!!
        }
    }

    /**
     * possible results:
     *
     * [NullPointerException] - getting user by id failed
     *
     * [String] - login
     */
    fun getLoginById(id: Long): Result<String> {
        return runCatching {
            getUserById(id).getOrThrow().login
        }.onFailure {
            println("getting login failed")
            it.printStackTrace()
        }
    }

    fun getRatingByLogin(login: String): Result<Long> {
        return runCatching {
            getUserByLogin(login).getOrThrow().rating
        }
    }

    fun getRatingById(id: Long): Result<Long> {
        return runCatching {
            getUserById(id).getOrThrow().rating
        }
    }

    /**
     * possible results:
     *
     *
     * [NullPointerException] - getting user by id failed
     *
     * [Triple] - date (Y, M, D)
     */
    fun getCreationDateById(id: Long): Result<Triple<Int, Int, Int>> {
        return runCatching {
            getUserById(id).getOrThrow().date
        }
    }

    fun setRatingByLogin(login: String, newRating: Long) {
        val user = getUserByLogin(login).getOrThrow()
        user.rating = newRating.coerceAtLeast(0)
        store()
    }

    fun updateRatingByLogin(login: String, deltaRating: Long) {
        val user = getUserByLogin(login).getOrThrow()
        user.rating = (deltaRating + user.rating).coerceAtLeast(0)
        store()
    }

    fun updateRatingById(id: Long, deltaRating: Long) {
        val user = getUserById(id).getOrThrow()
        user.rating = (deltaRating + user.rating).coerceAtLeast(0)
        store()
    }

    /**
     * possible results:
     *
     * [NullPointerException] - no user with such id
     *
     * [User] - user
     */
    private fun getUserById(id: Long): Result<User> {
        return runCatching {
            idToUsersMap[id]!!
        }.onFailure {
            println("getting user by id failed")
            it.printStackTrace()
        }
    }

    /**
     * possible results:
     *
     * [NullPointerException] - getting id by login failed | getting user by id failed
     *
     * [User] - user
     */
    private fun getUserByLogin(login: String): Result<User> {
        return runCatching {
            getUserById(getIdByLogin(login).getOrThrow()).getOrThrow()
        }
    }

    /**
     * possible results:
     *
     * [JWTDecodeException] - getting login failed
     *
     * [NullPointerException] - getting login failed | getting id by login failed
     *
     * [User] - user
     */
    fun getIdByJwtToken(jwtToken: String): Result<Long> {
        return runCatching {
            getIdByJwtToken(CustomJwtToken(jwtToken)).getOrThrow()
        }
    }

    /**
     * possible results:
     *
     * [JWTDecodeException] - getting login failed
     *
     * [NullPointerException] - getting login failed | getting id by login failed
     *
     * [User] - user
     */
    fun getIdByJwtToken(jwtToken: CustomJwtToken): Result<Long> {
        return runCatching {
            loginsToIdMap[jwtToken.getLogin().getOrThrow()]!!
        }.onFailure {
            println("login - ${jwtToken.getLogin().getOrNull()}")
            loginsToIdMap.forEach {
                println("key - ${it.key}, value - ${it.value}")
            }
        }
    }

    /**
     * true if login + password are in the database
     *
     * false otherwise
     */
    fun validateLoginData(login: String, password: String): Boolean {
        val jwtToken = CustomJwtToken(login, password)
        return validateJwtToken(jwtToken)
    }


    /**
     * true if user exist with provided login & password
     *
     * false if user doesn't exist | jwt token isn't proper
     */
    fun validateJwtToken(jwtToken: String): Boolean {
        val jwtTokenObject = CustomJwtToken(jwtToken)
        return validateJwtToken(jwtTokenObject)
    }

    /**
     * true if user exist with provided login & password
     *
     * false if user doesn't exist | jwt token isn't proper
     */
    fun validateJwtToken(jwtToken: CustomJwtToken): Boolean {
        val login = jwtToken.getLogin().getOrNull() ?: return false
        return getUserByLogin(login).getOrNull()?.jwtToken == jwtToken
    }

    /**
     * true if user with such login exists
     *
     * false otherwise
     */
    fun isLoginPresent(login: String): Boolean {
        return getIdByLogin(login).isSuccess
    }

    /**
     * possible results:
     *
     * [IllegalStateException] if user with the same login already exists
     *
     * [Unit] otherwise
     */
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
    /**
     * possible results:
     *
     * [IllegalStateException] if cookie isn't present
     *
     * [String] (jwt token) otherwise
     */
    fun login(login: String, password: String): Result<String> {
        return runCatching {
            if (!validateLoginData(login, password)) {
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
    @Serializable var profilePicture: ByteArray?
)

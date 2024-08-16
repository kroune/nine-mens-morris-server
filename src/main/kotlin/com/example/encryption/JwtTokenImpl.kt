package com.example.encryption

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.LogPriority
import com.example.currentConfig
import com.example.data.usersRepository
import com.example.log
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable

@Serializable
class JwtTokenImpl(val token: String) {
    constructor(login: String, password: String) : this(
        JWT.create()
            .withClaim("login", login)
            .withClaim("password", password)
            .withIssuedAt(Clock.System.now().toJavaInstant())
            .sign(Algorithm.HMAC256(currentConfig.encryptionToken))
    )

    /**
     * possible results:
     *
     * [JWTDecodeException] - error while decoding token
     *
     * [NullPointerException] - no login claim exists
     */
    fun getLogin(): Result<String> {
        return runCatching {
            val token = JWT.decode(token)
            token.claims["login"]!!.asString()
        }.onFailure {
            log("error decoding login", LogPriority.Info)
            it.printStackTrace()
        }
    }

    /**
     * possible results:
     *
     * [JWTDecodeException] - error while decoding token
     *
     * [NullPointerException] - no password_hash claim exists
     */
    private fun getPassword(): Result<String> {
        return runCatching {
            val token = JWT.decode(token)
            token.claims["password"]!!.asString()
        }.onFailure {
            log("error decoding login", LogPriority.Info)
            it.printStackTrace()
        }
    }

    suspend fun verify(): Boolean {
        val login = getLogin().getOrElse {
            return false
        }
        val password = getPassword().getOrElse {
            return false
        }
        return usersRepository.exists(login, password)
    }

    override fun equals(other: Any?): Boolean {
        error("don't compare jwt tokens")
    }
}

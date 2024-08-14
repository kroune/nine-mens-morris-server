package com.example.encryption

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.example.LogPriority
import com.example.currentConfig
import com.example.data.usersRepository
import com.example.log
import kotlinx.serialization.Serializable

@Serializable
class CustomJwtToken(var token: String = "") {
    constructor(login: String, password: String) : this(
        JWT.create()
            .withClaim("login", login)
            .withClaim("password", password)
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
            println("DEBUG: login error")
            return false
        }
        val password = getPassword().getOrElse {
            println("DEBUG: password hash error")
            return false
        }
        return usersRepository.exists(login, password)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CustomJwtToken) {
            error("don't compare jwt tokens with non jwt token")
        }
        val token1 = JWT.decode(token)
        val token2 = JWT.decode(other.token)
        val loginMatches = token1.claims["login"]?.asString() == token2.claims["login"]?.asString()
        val passwordMatches = token1.claims["password"]?.asString() == token2.claims["password"]?.asString()
        return loginMatches && passwordMatches
    }
}

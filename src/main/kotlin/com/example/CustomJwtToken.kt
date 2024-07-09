package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.routing.SECRET_SERVER_TOKEN
import kotlinx.serialization.Serializable
import java.time.Duration
import java.util.*

@Serializable
class CustomJwtToken(var token: String = "") {
    constructor(login: String, password: String) : this(
        JWT.create()
            .withClaim("login", login)
            .withClaim("password", password)
            .withExpiresAt(Date(System.currentTimeMillis() + Duration.ofHours(1L).toMillis()))
            .sign(Algorithm.HMAC256(SECRET_SERVER_TOKEN))
    )

    fun getLogin(): Result<String> {
        return runCatching {
            val token = JWT.decode(token)
            token.claims["login"]!!.asString()
        }
    }
    fun validate(login: String, password: String): Boolean {
        val token = JWT.decode(token)
        val loginMatches = token.claims["login"]?.asString() == login
        val passwordMatches = token.claims["password"]?.asString() == password
        return loginMatches && passwordMatches
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
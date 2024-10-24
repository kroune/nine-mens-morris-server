/*
 * This file is part of nine-mens-morris-server (https://github.com/kroune/nine-mens-morris-server)
 * Copyright (C) 2024-2024  kroune
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact: kr0ne@tuta.io
 */
package com.example.features.encryption

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.example.features.currentConfig
import com.example.data.local.usersRepository
import com.example.features.logging.log
import io.opentelemetry.api.logs.Severity
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
            log("error decoding login", Severity.WARN)
            log(it.stackTraceToString(), Severity.WARN)
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
            log("error decoding login", Severity.WARN)
            log(it.stackTraceToString(), Severity.WARN)
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

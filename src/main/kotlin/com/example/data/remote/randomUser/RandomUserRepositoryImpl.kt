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
package com.example.data.remote.randomUser

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RandomUserRepositoryImpl : RandomUserRepositoryI {
    private val network = HttpClient()
    private val jsonClient = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun getLoginAndPicture(): Result<Pair<String, ByteArray>> {
        return runCatching {
            val result = network.get("https://randomuser.me/api/?inc=login,picture").bodyAsText()
            val serviceData = jsonClient.decodeFromString<ServiceResponse>(result)
            val picture = network.get(serviceData.pictureUrl).body<ByteArray>()
            Pair(serviceData.username, picture)
        }
    }
}

@Serializable
private class ServiceResponse(val results: Array<results>) {
    val username: String
        get() {
            return results[0].login.username
        }

    val pictureUrl: String
        get() {
            return results[0].picture.medium
        }
}

@Serializable
private class results(val login: login, val picture: picture)

@Serializable
private class login(val username: String)

@Serializable
private class picture(val medium: String)
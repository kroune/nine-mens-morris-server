package com.example.api.randomUser

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
package com.example.userInfo.get

import com.example.TestDatabase
import com.example.applyPlugins
import com.example.data.local.usersRepository
import com.example.features.encryption.JwtTokenImpl
import com.example.routing.userInfo.get.userInfoRoutingGET
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test

class UserInfoGetTest {
    @Test
    fun `get login by id valid`() {
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            TestDatabase.connect()
            val user = TestDatabase.createDummyUser()
            val id = usersRepository.getIdByLogin(user.login)
            val request = client.get("/get-login-by-id") {
                this.parameter("id", id)
                this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
            }
            assert(request.status == HttpStatusCode.OK)
            assert(Json.decodeFromString<String>(request.bodyAsText()) == user.login)
        }
    }

    @Test
    fun `get login by id invalid id`() {
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            TestDatabase.connect()
            val user = TestDatabase.createDummyUser()
            val id = usersRepository.getIdByLogin(user.login)!!
            val request = client.get("/get-login-by-id") {
                this.parameter("id", id + 100L)
                this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
            }
            assert(request.status == HttpStatusCode.Forbidden)
        }
    }

    @Test
    fun `get login by id without id`() {
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            TestDatabase.connect()
            val user = TestDatabase.createDummyUser()
            val id = usersRepository.getIdByLogin(user.login)!!
            val request = client.get("/get-login-by-id") {
                this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
            }
            assert(request.status == HttpStatusCode.BadRequest)
        }
    }


    @Test
    fun `get creation date by id valid`() {
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            TestDatabase.connect()
            val user = TestDatabase.createDummyUser()
            val id = usersRepository.getIdByLogin(user.login)
            val request = client.get("/get-creation-date-by-id") {
                this.parameter("id", id)
                this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
            }
            assert(request.status == HttpStatusCode.OK)
            assert(Json.decodeFromString<Triple<Int, Int, Int>>(request.bodyAsText()) == user.date.let {
                Triple(it.dayOfMonth, it.monthNumber, it.year)
            })
        }
    }

    @Test
    fun `get creation date by id invalid id`() {
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            TestDatabase.connect()
            val user = TestDatabase.createDummyUser()
            val id = usersRepository.getIdByLogin(user.login)!!
            val request = client.get("/get-creation-date-by-id") {
                this.parameter("id", id + 100L)
                this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
            }
            assert(request.status == HttpStatusCode.Forbidden)
        }
    }

    @Test
    fun `get creation date by id without id`() {
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            TestDatabase.connect()
            val user = TestDatabase.createDummyUser()
            val id = usersRepository.getIdByLogin(user.login)!!
            val request = client.get("/get-creation-date-by-id")
            assert(request.status == HttpStatusCode.BadRequest)
        }
    }
}
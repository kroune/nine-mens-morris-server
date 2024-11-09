@file:Suppress("ClassName")

package com.example.userInfo.get

import com.example.TestDatabase
import com.example.applyPlugins
import com.example.data.local.users.UserData
import com.example.data.local.usersRepository
import com.example.features.encryption.JwtTokenImpl
import com.example.routing.userInfo.get.userInfoRoutingGET
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(Enclosed::class)
class UserInfoGetTest {
    class `get-login-by-id` {
        @Test
        fun `valid`() {
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
        fun `invalid id`() {
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
        fun `no id`() {
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
        fun `no jwt token`() {
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
    }

    class `get-creation-date-by-id` {
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

    class `leaderboard` {
        @Test
        fun `valid`() {
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                TestDatabase.connect()
                val usersInLeaderboard = mutableListOf<Long>()
                for (i in 1..14) {
                    val user = TestDatabase.createDummyUser(
                        userData = UserData(
                            "user$i",
                            password = "44444441s",
                            rating = 1000 + 2 * i
                        )
                    )
                    if (i > 4) {
                        usersInLeaderboard.add(usersRepository.getIdByLogin(user.login)!!)
                    }
                }
                usersInLeaderboard.sortDescending()
                val request = client.get("/leaderboard")
                assert(request.status == HttpStatusCode.OK)
                assert(Json.decodeFromString<List<Long>>(request.bodyAsText()) == usersInLeaderboard)
            }
        }
    }
}
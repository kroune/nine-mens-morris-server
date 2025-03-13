@file:Suppress("ClassName")

package com.example.userInfo.get

import com.example.TestDatabase
import com.example.applyPlugins
import com.example.data.local.users.InsertUserData
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(Enclosed::class)
class UserInfoGetTest {
    class `get-login-by-id` {
        @Test
        fun `valid request`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-login-by-id") {
                    parameter("id", id)
                    parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.OK)
                assertEquals(Json.decodeFromString<String>(request.bodyAsText()), user.login)
            }
        }

        @Test
        fun `invalid id`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)!!
                val request = client.get("/get-login-by-id") {
                    this.parameter("id", id + 100L)
                    this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `no id`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)!!
                val request = client.get("/get-login-by-id") {
                    this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.BadRequest)
            }
        }

        @Test
        fun `invalid jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-login-by-id") {
                    this.parameter("id", id)
                    this.parameter("jwtToken", JwtTokenImpl(user.login + "notValid", user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `no jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-login-by-id") {
                    this.parameter("id", id)
                }
                assertEquals(request.status, HttpStatusCode.BadRequest)
            }
        }

        @Test
        fun `id not a long`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-login-by-id") {
                    this.parameter("id", "notLong")
                    this.parameter("jwtToken", JwtTokenImpl(user.login + "notValid", user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }
    }

    class `get-creation-date-by-id` {
        @Test
        fun `valid request`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-creation-date-by-id") {
                    parameter("id", id)
                    parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.OK)
                assertEquals(Json.decodeFromString<Triple<Int, Int, Int>>(request.bodyAsText()), user.date.let {
                    Triple(it.dayOfMonth, it.monthNumber, it.year)
                })
            }
        }

        @Test
        fun `invalid id`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)!!
                val request = client.get("/get-creation-date-by-id") {
                    this.parameter("id", id + 100L)
                    this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `no id`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)!!
                val request = client.get("/get-creation-date-by-id") {
                    this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.BadRequest)
            }
        }

        @Test
        fun `id not a long`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-creation-date-by-id") {
                    this.parameter("id", "notLong")
                    this.parameter("jwtToken", JwtTokenImpl(user.login + "notValid", user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `invalid jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-creation-date-by-id") {
                    this.parameter("id", id)
                    this.parameter("jwtToken", JwtTokenImpl(user.login + "notValid", user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `no jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-creation-date-by-id") {
                    this.parameter("id", id)
                }
                assertEquals(request.status, HttpStatusCode.BadRequest)
            }
        }
    }

    class `get-rating-by-id` {
        @Test
        fun `valid request`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                val user = db.createDummyUser(InsertUserData("exampleLogin", "examplePass", rating = 3307))
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-rating-by-id") {
                    parameter("id", id)
                    parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.OK)
                assertEquals(Json.decodeFromString<Int>(request.bodyAsText()), user.rating)
            }
        }

        @Test
        fun `invalid id`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)!!
                val request = client.get("/get-rating-by-id") {
                    this.parameter("id", id + 100L)
                    this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `no id`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)!!
                val request = client.get("/get-rating-by-id") {
                    this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.BadRequest)
            }
        }

        @Test
        fun `id not a long`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-rating-by-id") {
                    this.parameter("id", "notLong")
                    this.parameter("jwtToken", JwtTokenImpl(user.login + "notValid", user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `invalid jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-rating-by-id") {
                    this.parameter("id", id)
                    this.parameter("jwtToken", JwtTokenImpl(user.login + "notValid", user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `no jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-rating-by-id") {
                    this.parameter("id", id)
                }
                assertEquals(request.status, HttpStatusCode.BadRequest)
            }
        }
    }

    class `get-id-by-jwt-token` {
        @Test
        fun `valid request`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser(InsertUserData("exampleLogin", "examplePass", rating = 3307))
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-id-by-jwt-token") {
                    parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.OK)
                assertEquals(Json.decodeFromString<Long>(request.bodyAsText()), id)
            }
        }

        @Test
        fun `invalid jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-id-by-jwt-token") {
                    this.parameter("jwtToken", JwtTokenImpl(user.login + "notValid", user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `no jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-id-by-jwt-token") {
                }
                assertEquals(request.status, HttpStatusCode.BadRequest)
            }
        }
    }

    class `get-picture-by-id` {
        @Test
        fun `valid request, no profile picture`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                val user = db.createDummyUser(InsertUserData("exampleLogin", "examplePass"))
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-picture-by-id") {
                    parameter("id", id)
                    parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.OK)
                assertContentEquals(
                    Json.decodeFromString<ByteArray>(request.bodyAsText()),
                    this.javaClass.getResource("/default_profile_image.png")!!.readBytes()
                )
            }
        }

        @Test
        fun `valid request, custom picture`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                val user = db.createDummyUser(
                    InsertUserData(
                        "exampleLogin",
                        "examplePass",
                        profilePicture = this.javaClass.getResource("/valid.png")!!.readBytes()
                    )
                )
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-picture-by-id") {
                    parameter("id", id)
                    parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.OK)
                assertContentEquals(
                    Json.decodeFromString<ByteArray>(request.bodyAsText()),
                    this.javaClass.getResource("/valid.png")!!.readBytes()
                )
            }
        }

        @Test
        fun `invalid id`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)!!
                val request = client.get("/get-picture-by-id") {
                    this.parameter("id", id + 100L)
                    this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `no id`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)!!
                val request = client.get("/get-picture-by-id") {
                    this.parameter("jwtToken", JwtTokenImpl(user.login, user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.BadRequest)
            }
        }

        @Test
        fun `id not a long`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-picture-by-id") {
                    this.parameter("id", "notLong")
                    this.parameter("jwtToken", JwtTokenImpl(user.login + "notValid", user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `invalid jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-picture-by-id") {
                    this.parameter("id", id)
                    this.parameter("jwtToken", JwtTokenImpl(user.login + "notValid", user.password).token)
                }
                assertEquals(request.status, HttpStatusCode.Forbidden)
            }
        }

        @Test
        fun `no jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                val user = db.createDummyUser()
                val id = usersRepository.getIdByLogin(user.login)
                val request = client.get("/get-picture-by-id") {
                    this.parameter("id", id)
                }
                assertEquals(request.status, HttpStatusCode.BadRequest)
            }
        }
    }

    class `leaderboard` {
        @Test
        fun `valid`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
                var jwtToken: String? = null
                for (i in 1..14) {
                    val user = db.createDummyUser(
                        userData = InsertUserData(
                            "user$i",
                            password = "44444441s",
                            rating = 1000 + 2 * i
                        )
                    )
                    jwtToken = JwtTokenImpl(user.login, user.password).token
                    if (i > 4) {
                        usersInLeaderboard.add(Pair(user.rating, usersRepository.getIdByLogin(user.login)!!))
                    }
                }
                usersInLeaderboard = usersInLeaderboard.sortedByDescending { it.first }.toMutableList()
                val request = client.get("/leaderboard") {
                    parameter("jwtToken", jwtToken)
                }
                assertEquals(request.status, HttpStatusCode.OK)
                assertEquals(
                    Json.decodeFromString<List<Long>>(request.bodyAsText()),
                    usersInLeaderboard.map { it.second })
            }
        }

        @Test
        fun `no jwt token`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
                var jwtToken: String? = null
                for (i in 1..14) {
                    val user = db.createDummyUser(
                        userData = InsertUserData(
                            "user$i",
                            password = "44444441s",
                            rating = 1000 + 2 * i
                        )
                    )
                    jwtToken = JwtTokenImpl(user.login, user.password).token
                    if (i > 4) {
                        usersInLeaderboard.add(Pair(user.rating, usersRepository.getIdByLogin(user.login)!!))
                    }
                }
                usersInLeaderboard = usersInLeaderboard.sortedByDescending { it.first }.toMutableList()
                val request = client.get("/leaderboard") {
                }
                assertEquals(request.status, HttpStatusCode.BadRequest)
            }
        }

        @Test
        fun `valid, less than 10 players`() {
            val db = TestDatabase()
            testApplication {
                application {
                    applyPlugins()
                }
                routing {
                    userInfoRoutingGET()
                }
                db.connect()
                var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
                var jwtToken: String? = null
                for (i in 1..4) {
                    val user = db.createDummyUser(
                        userData = InsertUserData(
                            "user$i",
                            password = "44444441s",
                            rating = 1000 + 2 * i
                        )
                    )
                    jwtToken = JwtTokenImpl(user.login, user.password).token
                    usersInLeaderboard.add(Pair(user.rating, usersRepository.getIdByLogin(user.login)!!))
                }
                usersInLeaderboard = usersInLeaderboard.sortedByDescending { it.first }.toMutableList()
                val request = client.get("/leaderboard") {
                    parameter("jwtToken", jwtToken)
                }
                assertEquals(request.status, HttpStatusCode.OK)
                assertEquals(
                    Json.decodeFromString<List<Long>>(request.bodyAsText()),
                    usersInLeaderboard.map { it.second })
            }
        }
    }
}
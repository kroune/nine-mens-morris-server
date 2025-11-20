@file:Suppress("ClassName")

package user.userInfo.get

import io.github.kroune.createDummyUser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext
import user.applyUserPlugins
import user.controller.userinfo.get.userInfoRoutingGET
import userApi.data.dao.UsersDataServiceI
import userApi.model.InsertUserPayload
import user.startTestDI
import kotlin.test.Test
import kotlin.test.assertEquals

class LeaderBoardTest {
    @Test
    fun `valid`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
            var jwtToken: String? = null
            for (i in 1..14) {
                val (user, jwt) = createDummyUser(
                    InsertUserPayload(
                        "user$i",
                        password = "44444441s",
                        rating = 1000 + 2 * i
                    )
                )
                jwtToken = jwt
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
    fun `valid with specified`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
            var jwtToken: String? = null
            for (i in 1..14) {
                val (user, jwt) = createDummyUser(
                    InsertUserPayload(
                        "user$i",
                        password = "44444441s",
                        rating = 1000 + 2 * i
                    )
                )
                jwtToken = jwt
                if (i > 4) {
                    usersInLeaderboard.add(Pair(user.rating, usersRepository.getIdByLogin(user.login)!!))
                }
            }
            usersInLeaderboard = usersInLeaderboard.sortedByDescending { it.first }.toMutableList()
            val request = client.get("/leaderboard") {
                parameter("jwtToken", jwtToken)
                parameter("amount", 10)
            }
            assertEquals(request.status, HttpStatusCode.OK)
            assertEquals(
                Json.decodeFromString<List<Long>>(request.bodyAsText()),
                usersInLeaderboard.map { it.second }
            )
            assert(Json.decodeFromString<List<Long>>(request.bodyAsText()).size == 10)
        }
    }

    @Test
    fun `underflow`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
            var jwtToken: String? = null
            for (i in 1..14) {
                val (user, jwt) = createDummyUser(
                    InsertUserPayload(
                        "user$i",
                        password = "44444441s",
                        rating = 1000 + 2 * i
                    )
                )
                jwtToken = jwt
                if (i > 4) {
                    usersInLeaderboard.add(Pair(user.rating, usersRepository.getIdByLogin(user.login)!!))
                }
            }
            usersInLeaderboard = usersInLeaderboard.sortedByDescending { it.first }.toMutableList()
            val request = client.get("/leaderboard") {
                parameter("jwtToken", jwtToken)
                parameter("amount", -5)
            }
            assertEquals(request.status, HttpStatusCode.OK)
            assertEquals(
                Json.decodeFromString<List<Long>>(request.bodyAsText()),
                listOf()
            )
            assert(Json.decodeFromString<List<Long>>(request.bodyAsText()).isEmpty())
        }
    }

    @Test
    fun `min`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
            var jwtToken: String? = null
            for (i in 1..14) {
                val (user, jwt) = createDummyUser(
                    InsertUserPayload(
                        "user$i",
                        password = "44444441s",
                        rating = 1000 + 2 * i
                    )
                )
                jwtToken = jwt
                if (i > 13) {
                    usersInLeaderboard.add(Pair(user.rating, usersRepository.getIdByLogin(user.login)!!))
                }
            }
            usersInLeaderboard = usersInLeaderboard.sortedByDescending { it.first }.toMutableList()
            val request = client.get("/leaderboard") {
                parameter("jwtToken", jwtToken)
                parameter("amount", 1)
            }
            assertEquals(request.status, HttpStatusCode.OK)
            assertEquals(
                Json.decodeFromString<List<Long>>(request.bodyAsText()),
                usersInLeaderboard.map { it.second }
            )
            assert(Json.decodeFromString<List<Long>>(request.bodyAsText()).size == 1)
        }
    }

    @Test
    fun `max`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
            var jwtToken: String? = null
            for (i in 1..114) {
                val (user, jwt) = createDummyUser(
                    InsertUserPayload(
                        "user$i",
                        password = "44444441s",
                        rating = 1000 + 2 * i
                    )
                )
                jwtToken = jwt
                if (i > 14) {
                    usersInLeaderboard.add(Pair(user.rating, usersRepository.getIdByLogin(user.login)!!))
                }
            }
            usersInLeaderboard = usersInLeaderboard.sortedByDescending { it.first }.toMutableList()
            val request = client.get("/leaderboard") {
                parameter("jwtToken", jwtToken)
                parameter("amount", 100)
            }
            assertEquals(request.status, HttpStatusCode.OK)
            assertEquals(
                Json.decodeFromString<List<Long>>(request.bodyAsText()),
                usersInLeaderboard.map { it.second }
            )
            assert(Json.decodeFromString<List<Long>>(request.bodyAsText()).size == 100)
        }
    }

    @Test
    fun `overflow`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
            var jwtToken: String? = null
            for (i in 1..114) {
                val (user, jwt) = createDummyUser(
                    InsertUserPayload(
                        "user$i",
                        password = "44444441s",
                        rating = 1000 + 2 * i
                    )
                )
                jwtToken = jwt
                if (i > 14) {
                    usersInLeaderboard.add(Pair(user.rating, usersRepository.getIdByLogin(user.login)!!))
                }
            }
            usersInLeaderboard = usersInLeaderboard.sortedByDescending { it.first }.toMutableList()
            val request = client.get("/leaderboard") {
                parameter("jwtToken", jwtToken)
                parameter("amount", 110)
            }
            assertEquals(request.status, HttpStatusCode.OK)
            assertEquals(
                Json.decodeFromString<List<Long>>(request.bodyAsText()),
                usersInLeaderboard.map { it.second }
            )
            assert(Json.decodeFromString<List<Long>>(request.bodyAsText()).size == 100)
        }
    }

    @Test
    fun `no jwt token`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
            var jwtToken: String? = null
            for (i in 1..14) {
                val (user, jwt) = createDummyUser(
                    InsertUserPayload(
                        "user$i",
                        password = "44444441s",
                        rating = 1000 + 2 * i
                    )
                )
                jwtToken = jwt
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
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            var usersInLeaderboard = mutableListOf<Pair<Int, Long>>()
            var jwtToken: String? = null
            for (i in 1..4) {
                val (user, jwt) = createDummyUser(
                    InsertUserPayload(
                        "user$i",
                        password = "44444441s",
                        rating = 1000 + 2 * i
                    )
                )
                jwtToken = jwt
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
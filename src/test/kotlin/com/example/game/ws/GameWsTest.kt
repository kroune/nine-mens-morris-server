package com.example.game.ws

import com.example.TestDatabase
import com.example.applyPlugins
import com.example.data.local.users.InsertUserData
import com.example.features.encryption.JwtTokenImpl
import com.example.routing.game.ws.gameRoutingWS
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.testing.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class GameWsTest {
    class `search-for-game` {
        @Test
        fun `connect with real player and join`() {
            testApplication {
                val db = TestDatabase()
                val client2 = createClient {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(Json)
                        pingInterval = 3.seconds
                    }
                }
                application {
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val user1 = db.createDummyUser(InsertUserData("user1", "password1"))
                val user2 = db.createDummyUser(InsertUserData("user2", "password2"))
                runBlocking {
                    withTimeoutOrNull(100.seconds) {
                        var firstGameId: Long? = null
                        var secondGameId: Long? = null
                        val job1 = CoroutineScope(Dispatchers.IO).launch {
                            client2.ws(urlString = "/search-for-game", request = {
                                url {
                                    parameter("jwtToken", JwtTokenImpl(user1.login, user1.password).token)
                                }
                            }) {
                                while (true) {
                                    val info = receiveDeserialized<Pair<Boolean, Long>>()
                                    if (!info.first) {
                                        firstGameId = info.second
                                        println("game id = ${info.second}")
                                        break
                                    }
                                }
                            }
                            if (secondGameId != null) {
                                assert(secondGameId == firstGameId)
                            }
                        }
                        delay(5.seconds)
                        val job2 = CoroutineScope(Dispatchers.IO).launch {
                            client2.ws(urlString = "/search-for-game", request = {
                                url {
                                    parameter("jwtToken", JwtTokenImpl(user2.login, user2.password).token)
                                }
                            }) {
                                while (true) {
                                    val info = receiveDeserialized<Pair<Boolean, Long>>()
                                    if (!info.first) {
                                        secondGameId = info.second
                                        println("game id = ${info.second}")
                                        break
                                    }
                                }
                            }
                            if (secondGameId != null) {
                                assert(secondGameId == firstGameId)
                            }
                        }
                        listOf(job1, job2).forEach { it.join() }
                        assert(secondGameId == firstGameId)
                        println("firstGameId = $firstGameId, secondGameId = $secondGameId")
                        client2.ws(urlString = "/game", request = {
                            url {
                                parameter("jwtToken", JwtTokenImpl(user1.login, user1.password).token)
                            }
                        }) {
                            closeReason.await()
                        }
                    }!!
                }
            }
        }

        @Test
        fun `connect with real player`() {
            testApplication {
                val db = TestDatabase()
                val client2 = createClient {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(Json)
                        pingInterval = 3.seconds
                    }
                }
                application {
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val user1 = db.createDummyUser(InsertUserData("user1", "password1"))
                val user2 = db.createDummyUser(InsertUserData("user2", "password2"))
                runBlocking {
                    withTimeoutOrNull(100.seconds) {
                        var firstGameId: Long? = null
                        var secondGameId: Long? = null
                        val job1 = CoroutineScope(Dispatchers.IO).launch {
                            client2.ws(urlString = "/search-for-game", request = {
                                url {
                                    parameter("jwtToken", JwtTokenImpl(user1.login, user1.password).token)
                                }
                            }) {
                                while (true) {
                                    val info = receiveDeserialized<Pair<Boolean, Long>>()
                                    if (!info.first) {
                                        firstGameId = info.second
                                        println("game id = ${info.second}")
                                        break
                                    }
                                }
                            }
                            if (secondGameId != null) {
                                assert(secondGameId == firstGameId)
                            }
                        }
                        delay(5.seconds)
                        val job2 = CoroutineScope(Dispatchers.IO).launch {
                            client2.ws(urlString = "/search-for-game", request = {
                                url {
                                    parameter("jwtToken", JwtTokenImpl(user2.login, user2.password).token)
                                }
                            }) {
                                while (true) {
                                    val info = receiveDeserialized<Pair<Boolean, Long>>()
                                    if (!info.first) {
                                        secondGameId = info.second
                                        println("game id = ${info.second}")
                                        break
                                    }
                                }
                            }
                            if (secondGameId != null) {
                                assert(secondGameId == firstGameId)
                            }
                        }
                        listOf(job1, job2).forEach { it.join() }
                        assert(secondGameId == firstGameId)
                        println("firstGameId = $firstGameId, secondGameId = $secondGameId")
                    }!!
                }
            }
        }

        @Test
        fun `connect with bot`() {
            testApplication {
                val db = TestDatabase()
                val client2 = createClient {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(Json)
                        pingInterval = 3.seconds
                    }
                }
                application {
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val user1 = db.createDummyUser(InsertUserData("user1", "password1"))
                runBlocking {
                    withTimeoutOrNull(100.seconds) {
                        var firstGameId: Long? = null
                        val job1 = CoroutineScope(Dispatchers.IO).launch {
                            client2.ws(urlString = "/search-for-game", request = {
                                url {
                                    parameter("jwtToken", JwtTokenImpl(user1.login, user1.password).token)
                                }
                            }) {
                                while (true) {
                                    val info = receiveDeserialized<Pair<Boolean, Long>>()
                                    if (!info.first) {
                                        firstGameId = info.second
                                        println("game id = ${info.second}")
                                        break
                                    }
                                }
                            }
                        }
                        listOf(job1).forEach { it.join() }
                        println(
                            "firstGameId = $firstGameId" +
                                    ""
                        )
                    }!!
                }
            }
        }
    }
}
package io.github.kroune.game.ws

import io.github.kroune.TestDatabase
import io.github.kroune.applyPlugins
import io.github.kroune.data.local.users.InsertUserData
import io.github.kroune.features.encryption.JwtTokenImpl
import io.github.kroune.routing.game.ws.gameRoutingWS
import io.github.kroune.startDI
import getGameId
import io.ktor.server.testing.*
import kotlinx.coroutines.*
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
                application {
                    startDI()
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val user1 = db.createDummyUser(InsertUserData("user1", "password1"))
                val user2 = db.createDummyUser(InsertUserData("user2", "password2"))
                var gameId1: Deferred<Long>
                var gameId2: Deferred<Long>
                runBlocking {
                    withTimeout(100.seconds) {
                        gameId1 = this@testApplication.getGameId(JwtTokenImpl(user1.login, user1.password).token)
                        delay(5.seconds)
                        gameId2 = this@testApplication.getGameId(JwtTokenImpl(user2.login, user2.password).token)
                        assert(gameId2.await() == gameId1.await())
                        println("firstGameId = ${gameId1.await()}, secondGameId = ${gameId2.await()}")
                    }
                }
                var gameId3 = this@testApplication.getGameId(JwtTokenImpl(user1.login, user1.password).token)
                assert(gameId1.await() == gameId3.await())
            }
        }

        @Test
        fun `connect with real player`() {
            testApplication {
                val db = TestDatabase()
                application {
                    startDI()
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val user1 = db.createDummyUser(InsertUserData("user1", "password1"))
                val user2 = db.createDummyUser(InsertUserData("user2", "password2"))
                runBlocking {
                    withTimeout(100.seconds) {
                        var firstGameId: Deferred<Long> = getGameId(JwtTokenImpl(user1.login, user1.password).token)
                        delay(5.seconds)
                        var secondGameId: Deferred<Long> = getGameId(JwtTokenImpl(user2.login, user2.password).token)
                        assert(secondGameId.await() == firstGameId.await())
                        println("firstGameId = ${firstGameId.await()}, secondGameId = ${secondGameId.await()}")
                    }
                }
            }
        }

        @Test
        fun `connect with bot`() {
            testApplication {
                val db = TestDatabase()
                application {
                    startDI()
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val user1 = db.createDummyUser(InsertUserData("user1", "password1"))
                runBlocking {
                    withTimeout(100.seconds) {
                        var firstGameId: Deferred<Long> = getGameId(JwtTokenImpl(user1.login, user1.password).token)
                        println(
                            "firstGameId = ${firstGameId.await()}" +
                                    ""
                        )
                    }
                }
            }
        }
    }
}
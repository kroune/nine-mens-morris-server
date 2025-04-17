package io.github.kroune.game.ws

import getGameId
import io.github.kroune.TestDatabase
import io.github.kroune.applyPlugins
import data.InsertUserData
import io.github.kroune.routing.game.ws.gameRoutingWS
import io.ktor.server.testing.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import startTestDI
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
                    startTestDI()
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val (_, jwt1) = db.createDummyUser(InsertUserData("user1", "password1"))
                val (_, jwt2) = db.createDummyUser(InsertUserData("user2", "password2"))
                var gameId1: Deferred<Long>
                var gameId2: Deferred<Long>
                runBlocking {
                    withTimeout(100.seconds) {
                        gameId1 = this@testApplication.getGameId(jwt1).first
                        delay(5.seconds)
                        gameId2 = this@testApplication.getGameId(jwt2).first
                        assert(gameId2.await() == gameId1.await())
                        println("firstGameId = ${gameId1.await()}, secondGameId = ${gameId2.await()}")
                    }
                }
                var gameId3 = this@testApplication.getGameId(jwt1).first
                assert(gameId1.await() == gameId3.await())
            }
        }

        @Test
        fun `connect with real player`() {
            testApplication {
                val db = TestDatabase()
                application {
                    startTestDI()
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val (_, jwt1) = db.createDummyUser(InsertUserData("user1", "password1"))
                val (_, jwt2) = db.createDummyUser(InsertUserData("user2", "password2"))
                runBlocking {
                    withTimeout(100.seconds) {
                        var firstGameId: Deferred<Long> = getGameId(jwt1).first
                        delay(5.seconds)
                        var secondGameId: Deferred<Long> = getGameId(jwt2).first
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
                    startTestDI()
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val (_, jwt1) = db.createDummyUser(InsertUserData("user1", "password1"))
                runBlocking {
                    withTimeout(100.seconds) {
                        var firstGameId: Deferred<Long> = getGameId(jwt1).first
                        println(
                            "firstGameId = ${firstGameId.await()}" +
                                    ""
                        )
                    }
                }
            }
        }
        @Test
        fun `verify waiting time`() {
            testApplication {
                val db = TestDatabase()
                application {
                    startTestDI()
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val (_, jwt1) = db.createDummyUser(InsertUserData("user1", "password1"))
                runBlocking {
                    withTimeout(100.seconds) {
                        var (firstGameId, channel) = getGameId(jwt1)
                        println(
                            "firstGameId = ${firstGameId.await()}" +
                                    ""
                        )
                        assert(channel.consumeAsFlow().count() > 3)
                    }
                }
            }
        }

        @Test
        fun `connect with already present game`() {
            testApplication {
                val db = TestDatabase()
                application {
                    startTestDI()
                    applyPlugins()
                }
                routing {
                    gameRoutingWS()
                }
                db.connect()
                val (_, jwt1) = db.createDummyUser(InsertUserData("user1", "password1"))
                val (_, jwt2) = db.createDummyUser(InsertUserData("user2", "password2"))
                runBlocking {
                    var firstGameId: Deferred<Long> = getGameId(jwt1).first
                    delay(5.seconds)
                    var secondGameId: Deferred<Long> = getGameId(jwt2).first
                    assert(secondGameId.await() == firstGameId.await())
                    println("firstGameId = ${firstGameId.await()}, secondGameId = ${secondGameId.await()}")
                    repeat(5) {
                        var firstGameIdSecondAttempt: Deferred<Long> = getGameId(jwt1).first
                        assert(firstGameId.await() == firstGameIdSecondAttempt.await())
                    }
                }
            }
        }
    }
}
import user.data.InsertUserPayload
import commonTests.TestKafka
import gameMain.applyGamePlugins
import io.github.kroune.createDummyUser
import io.ktor.server.testing.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import gameMain.routing.gameMainRouting
import gameQueue.routing.gameQueueRouting
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class GameWsTest {
    @Test
    fun `connect with real player and join`() {
        testApplication {
            application {
                startTestDI()
                applyGamePlugins()
            }
            routing {
                gameMainRouting()
                gameQueueRouting()
            }
            startApplication()
            TestKafka

            val (_, jwt1) = createDummyUser(InsertUserPayload("user1", "password1"))
            val (_, jwt2) = createDummyUser(InsertUserPayload("user2", "password2"))

            runBlocking {
                var gameId1: Deferred<Long> = this@testApplication.getGameId(jwt1).first
                delay(5.seconds)
                var gameId2: Deferred<Long> = this@testApplication.getGameId(jwt2).first
                assert(gameId2.await() == gameId1.await())
                println("firstGameId = ${gameId1.await()}, secondGameId = ${gameId2.await()}")
                var gameId3 = this@testApplication.getGameId(jwt1).first
                assert(gameId1.await() == gameId3.await())
            }
        }
    }

    @Test
    fun `connect with real player after delay`() {
        testApplication {
            application {
                startTestDI()
                applyGamePlugins()
            }
            routing {
                gameMainRouting()
                gameQueueRouting()
            }
            startApplication()

            TestKafka

            runBlocking {
                val (_, jwt1) = createDummyUser(InsertUserPayload("user1", "password1"))
                val (_, jwt2) = createDummyUser(InsertUserPayload("user2", "password2"))
                var firstGameId: Deferred<Long> = getGameId(jwt1).first
                delay(5.seconds)
                var secondGameId: Deferred<Long> = getGameId(jwt2).first
                assert(secondGameId.await() == firstGameId.await())
                println("firstGameId = ${firstGameId.await()}, secondGameId = ${secondGameId.await()}")
            }
        }
    }

    @Test
    fun `connect with bot`() {
        testApplication {
            application {
                startTestDI()
                applyGamePlugins()
            }
            routing {
                gameMainRouting()
                gameQueueRouting()
            }
            startApplication()

            TestKafka

            runBlocking {
                val (_, jwt1) = createDummyUser(InsertUserPayload("user1", "password1"))
                var firstGameId: Deferred<Long> = getGameId(jwt1).first
                println(
                    "firstGameId = ${firstGameId.await()}" +
                            ""
                )
            }
        }
    }

    @Test
    fun `verify waiting time`() {
        testApplication {
            application {
                startTestDI()
                applyGamePlugins()
            }
            routing {
                gameMainRouting()
                gameQueueRouting()
            }
            startApplication()

            TestKafka

            runBlocking {
                val (_, jwt1) = createDummyUser(InsertUserPayload("user1", "password1"))
                var (firstGameId, channel) = getGameId(jwt1)
                println(
                    "firstGameId = ${firstGameId.await()}" +
                            ""
                )
                assert(channel.consumeAsFlow().count() > 3)
            }
        }
    }

    @Test
    fun `connect with already present game`() {
        testApplication {
            application {
                startTestDI()
                applyGamePlugins()
            }
            routing {
                gameMainRouting()
                gameQueueRouting()
            }
            startApplication()

            TestKafka

            runBlocking {
                val (_, jwt1) = createDummyUser(InsertUserPayload("user1", "password1"))
                val (_, jwt2) = createDummyUser(InsertUserPayload("user2", "password2"))
                var firstGameId: Deferred<Long> = getGameId(jwt1).first
                delay(5.seconds)
                var secondGameId: Deferred<Long> = getGameId(jwt2).first
                assert(secondGameId.await() == firstGameId.await())
                println("firstGameId = ${firstGameId.await()}, secondGameId = ${secondGameId.await()}")
                repeat(5) {
                    var firstGameIdSecondAttempt: Deferred<Long> = getGameId(jwt1).first
                    assert(firstGameId.await() == firstGameIdSecondAttempt.await())
                    println("check passed")
                }
            }
        }
    }
}
package io.github.kroune.userInfo.post

import io.github.kroune.TestDatabase
import io.github.kroune.applyPlugins
import io.github.kroune.data.local.usersRepository
import io.github.kroune.routing.userInfo.post.userInfoRoutingPOST
import io.github.kroune.startDI
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(Enclosed::class)
class UserInfoPostTest {
    class `upload picture` {
        @Test
        fun `upload valid picture`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    startDI()
                    applyPlugins()
                }
                routing {
                    userInfoRoutingPOST()
                }
                val (_, jwt) = db.createDummyUser()
                val validImage = this.javaClass.getResource("/valid.png")!!
                val result = client.post("/upload-picture") {
                    this.parameter("jwtToken", jwt)
                    this.setBody(validImage.readBytes())
                }
                assertEquals(result.status, HttpStatusCode.OK)
                val id = usersRepository.getIdByJwtToken(jwt)!!
                val pictureFromDb = usersRepository.getPictureById(id)
                assertContentEquals(validImage.readBytes(), pictureFromDb)
            }
        }

        @Test
        fun `upload corrupted picture`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    startDI()
                    applyPlugins()
                }
                routing {
                    userInfoRoutingPOST()
                }
                val (_, jwt) = db.createDummyUser()
                val validImage = this.javaClass.getResource("/corrupted.png")!!
                val result = client.post("/upload-picture") {
                    this.parameter("jwtToken", jwt)
                    this.setBody(validImage.readBytes())
                }
                assertEquals(result.status, HttpStatusCode.Forbidden)
                val id = usersRepository.getIdByJwtToken(jwt)!!
                val pictureFromDb = usersRepository.getPictureById(id)
                assertNotEquals(validImage.readBytes(), pictureFromDb)
            }
        }

        @Test
        fun `upload too big picture`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    startDI()
                    applyPlugins()
                }
                routing {
                    userInfoRoutingPOST()
                }
                val (_, jwt) = db.createDummyUser()
                val validImage = this.javaClass.getResource("/tooBig.png")!!
                val result = client.post("/upload-picture") {
                    this.parameter("jwtToken", jwt)
                    this.setBody(validImage.readBytes())
                }
                assertEquals(result.status, HttpStatusCode.BadRequest)
                val id = usersRepository.getIdByJwtToken(jwt)!!
                val pictureFromDb = usersRepository.getPictureById(id)
                assertNotEquals(validImage.readBytes(), pictureFromDb)
            }
        }

        @Test
        fun `upload picture with invalid jwt token`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    startDI()
                    applyPlugins()
                }
                routing {
                    userInfoRoutingPOST()
                }
                val (_, jwt) = db.createDummyUser()
                val validImage = this.javaClass.getResource("/valid.png")!!
                val result = client.post("/upload-picture") {
                    this.parameter("jwtToken", "acxzczx$jwt")
                    this.setBody(validImage.readBytes())
                }
                assertEquals(result.status, HttpStatusCode.Forbidden)
                val id = usersRepository.getIdByJwtToken(jwt)!!
                val pictureFromDb = usersRepository.getPictureById(id)
                assertNotEquals(validImage.readBytes(), pictureFromDb)
            }
        }

        @Test
        fun `upload picture without jwt token`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    startDI()
                    applyPlugins()
                }
                routing {
                    userInfoRoutingPOST()
                }
                val (_, jwt) = db.createDummyUser()
                val validImage = this.javaClass.getResource("/valid.png")!!
                val result = client.post("/upload-picture") {
                    this.setBody(validImage.readBytes())
                }
                assertEquals(result.status, HttpStatusCode.BadRequest)
                val id = usersRepository.getIdByJwtToken(jwt)!!
                val pictureFromDb = usersRepository.getPictureById(id)
                assertNotEquals(validImage.readBytes(), pictureFromDb)
            }
        }

        @Test
        fun `body not a byte array`() {
            val db = TestDatabase()
            db.connect()
            testApplication {
                application {
                    startDI()
                    applyPlugins()
                }
                routing {
                    userInfoRoutingPOST()
                }
                val (_, jwt) = db.createDummyUser()
                val result = client.post("/upload-picture") {
                    this.parameter("jwtToken", jwt)
                    this.setBody("not an image")
                }
                assertEquals(result.status, HttpStatusCode.Forbidden)
            }
        }
    }
}
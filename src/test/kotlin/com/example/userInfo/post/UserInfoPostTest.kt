package com.example.userInfo.post

import com.example.TestDatabase
import com.example.applyPlugins
import com.example.data.local.usersRepository
import com.example.features.encryption.JwtTokenImpl
import com.example.routing.userInfo.post.userInfoRoutingPOST
import com.example.startDI
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
                val user = db.createDummyUser()
                val jwtToken = JwtTokenImpl(user.login, user.password)
                val validImage = this.javaClass.getResource("/valid.png")!!
                val result = client.post("/upload-picture") {
                    this.parameter("jwtToken", jwtToken.token)
                    this.setBody(validImage.readBytes())
                }
                assertEquals(result.status, HttpStatusCode.OK)
                val id = usersRepository.getIdByJwtToken(jwtToken.token)!!
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
                val user = db.createDummyUser()
                val jwtToken = JwtTokenImpl(user.login, user.password)
                val validImage = this.javaClass.getResource("/corrupted.png")!!
                val result = client.post("/upload-picture") {
                    this.parameter("jwtToken", jwtToken.token)
                    this.setBody(validImage.readBytes())
                }
                assertEquals(result.status, HttpStatusCode.Forbidden)
                val id = usersRepository.getIdByJwtToken(jwtToken.token)!!
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
                val user = db.createDummyUser()
                val jwtToken = JwtTokenImpl(user.login, user.password)
                val validImage = this.javaClass.getResource("/tooBig.png")!!
                val result = client.post("/upload-picture") {
                    this.parameter("jwtToken", jwtToken.token)
                    this.setBody(validImage.readBytes())
                }
                assertEquals(result.status, HttpStatusCode.BadRequest)
                val id = usersRepository.getIdByJwtToken(jwtToken.token)!!
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
                val user = db.createDummyUser()
                val jwtToken = JwtTokenImpl(user.login, user.password)
                val validImage = this.javaClass.getResource("/valid.png")!!
                val result = client.post("/upload-picture") {
                    this.parameter("jwtToken", "acxzczx" + jwtToken.token)
                    this.setBody(validImage.readBytes())
                }
                assertEquals(result.status, HttpStatusCode.Forbidden)
                val id = usersRepository.getIdByJwtToken(jwtToken.token)!!
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
                val user = db.createDummyUser()
                val jwtToken = JwtTokenImpl(user.login, user.password)
                val validImage = this.javaClass.getResource("/valid.png")!!
                val result = client.post("/upload-picture") {
                    this.setBody(validImage.readBytes())
                }
                assertEquals(result.status, HttpStatusCode.BadRequest)
                val id = usersRepository.getIdByJwtToken(jwtToken.token)!!
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
                val user = db.createDummyUser()
                val jwtToken = JwtTokenImpl(user.login, user.password)
                val validImage = this.javaClass.getResource("/valid.png")!!
                val result = client.post("/upload-picture") {
                    this.parameter("jwtToken", jwtToken.token)
                    this.setBody("not an image")
                }
                assertEquals(result.status, HttpStatusCode.Forbidden)
            }
        }
    }
}
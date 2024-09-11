package com.example.userInfo.post

import com.example.TestDatabase
import com.example.applyPlugins
import com.example.data.usersRepository
import com.example.encryption.JwtTokenImpl
import com.example.routing.userInfo.post.userInfoRoutingPOST
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UserInfoPostTest {
    @Test
    fun `upload valid picture`() {
        TestDatabase.connect()
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            val user = TestDatabase.createDummyUser()
            val jwtToken = JwtTokenImpl(user.login, user.password)
            val validImage = this.javaClass.getResource("/valid.png")!!
            val result = client.post("/upload-picture") {
                this.parameter("jwtToken", jwtToken.token)
                this.setBody(validImage.readBytes())
            }
            assertTrue(result.status == HttpStatusCode.OK)
            val id = usersRepository.getIdByJwtToken(jwtToken.token)!!
            val pictureFromDb = usersRepository.getPictureById(id)
            assertContentEquals(validImage.readBytes(), pictureFromDb)
        }
    }

    @Test
    fun `upload corrupted picture`() {
        TestDatabase.connect()
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            val user = TestDatabase.createDummyUser()
            val jwtToken = JwtTokenImpl(user.login, user.password)
            val validImage = this.javaClass.getResource("/corrupted.png")!!
            val result = client.post("/upload-picture") {
                this.parameter("jwtToken", jwtToken.token)
                this.setBody(validImage.readBytes())
            }
            assertTrue(result.status == HttpStatusCode.Forbidden)
            val id = usersRepository.getIdByJwtToken(jwtToken.token)!!
            val pictureFromDb = usersRepository.getPictureById(id)
            assertNotEquals(validImage.readBytes(), pictureFromDb)
        }
    }

    @Test
    fun `upload too big picture`() {
        TestDatabase.connect()
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            val user = TestDatabase.createDummyUser()
            val jwtToken = JwtTokenImpl(user.login, user.password)
            val validImage = this.javaClass.getResource("/tooBig.png")!!
            val result = client.post("/upload-picture") {
                this.parameter("jwtToken", jwtToken.token)
                this.setBody(validImage.readBytes())
            }
            assertTrue(result.status == HttpStatusCode.Forbidden)
            val id = usersRepository.getIdByJwtToken(jwtToken.token)!!
            val pictureFromDb = usersRepository.getPictureById(id)
            assertNotEquals(validImage.readBytes(), pictureFromDb)
        }
    }

    @Test
    fun `upload picture with invalid jwt token`() {
        TestDatabase.connect()
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            val user = TestDatabase.createDummyUser()
            val jwtToken = JwtTokenImpl(user.login, user.password)
            val validImage = this.javaClass.getResource("/valid.png")!!
            val result = client.post("/upload-picture") {
                this.parameter("jwtToken", "acxzczx" + jwtToken.token)
                this.setBody(validImage.readBytes())
            }
            assertTrue(result.status == HttpStatusCode.Forbidden)
            val id = usersRepository.getIdByJwtToken(jwtToken.token)!!
            val pictureFromDb = usersRepository.getPictureById(id)
            assertNotEquals(validImage.readBytes(), pictureFromDb)
        }
    }

    @Test
    fun `upload picture without jwt token`() {
        TestDatabase.connect()
        testApplication {
            application {
                applyPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            val user = TestDatabase.createDummyUser()
            val jwtToken = JwtTokenImpl(user.login, user.password)
            val validImage = this.javaClass.getResource("/valid.png")!!
            val result = client.post("/upload-picture") {
                this.setBody(validImage.readBytes())
            }
            assertTrue(result.status == HttpStatusCode.BadRequest)
            val id = usersRepository.getIdByJwtToken(jwtToken.token)!!
            val pictureFromDb = usersRepository.getPictureById(id)
            assertNotEquals(validImage.readBytes(), pictureFromDb)
        }
    }
}
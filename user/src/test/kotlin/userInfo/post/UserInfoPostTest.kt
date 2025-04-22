package userInfo.post

import user.data.dao.UsersDataServiceI
import commonTests.TestDatabase
import user.applyUserPlugins
import io.github.kroune.createDummyUser
import user.routing.userinfo.post.userInfoRoutingPOST
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.koin.core.context.GlobalContext
import startTestDI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UserInfoPostTest {
    @Test
    fun `upload valid picture`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            startApplication()
            val db = TestDatabase.apply {
                create()
            }
            val (_, jwt) = db.createDummyUser()
            val validImage = this.javaClass.getResource("/valid.png")!!
            val result = client.post("/upload-picture") {
                this.parameter("jwtToken", jwt)
                this.setBody(validImage.readBytes())
            }
            assertEquals(result.status, HttpStatusCode.OK)
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByJwtToken(jwt)!!
            val pictureFromDb = usersRepository.getPictureById(id)
            assertContentEquals(validImage.readBytes(), pictureFromDb)
        }
    }

    @Test
    fun `upload corrupted picture`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            startApplication()
            val db = TestDatabase.apply {
                create()
            }
            val (_, jwt) = db.createDummyUser()
            val validImage = this.javaClass.getResource("/corrupted.png")!!
            val result = client.post("/upload-picture") {
                this.parameter("jwtToken", jwt)
                this.setBody(validImage.readBytes())
            }
            assertEquals(result.status, HttpStatusCode.Forbidden)
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByJwtToken(jwt)!!
            val pictureFromDb = usersRepository.getPictureById(id)
            assertNotEquals(validImage.readBytes(), pictureFromDb)
        }
    }

    @Test
    fun `upload too big picture`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            startApplication()
            val db = TestDatabase.apply {
                create()
            }
            val (_, jwt) = db.createDummyUser()
            val validImage = this.javaClass.getResource("/tooBig.png")!!
            val result = client.post("/upload-picture") {
                this.parameter("jwtToken", jwt)
                this.setBody(validImage.readBytes())
            }
            assertEquals(result.status, HttpStatusCode.BadRequest)
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByJwtToken(jwt)!!
            val pictureFromDb = usersRepository.getPictureById(id)
            assertNotEquals(validImage.readBytes(), pictureFromDb)
        }
    }

    @Test
    fun `upload picture with invalid jwt token`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            startApplication()
            val db = TestDatabase.apply {
                create()
            }
            val (_, jwt) = db.createDummyUser()
            val validImage = this.javaClass.getResource("/valid.png")!!
            val result = client.post("/upload-picture") {
                this.parameter("jwtToken", "acxzczx$jwt")
                this.setBody(validImage.readBytes())
            }
            assertEquals(result.status, HttpStatusCode.Forbidden)
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByJwtToken(jwt)!!
            val pictureFromDb = usersRepository.getPictureById(id)
            assertNotEquals(validImage.readBytes(), pictureFromDb)
        }
    }

    @Test
    fun `upload picture without jwt token`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            startApplication()
            val db = TestDatabase.apply {
                create()
            }
            val (_, jwt) = db.createDummyUser()
            val validImage = this.javaClass.getResource("/valid.png")!!
            val result = client.post("/upload-picture") {
                this.setBody(validImage.readBytes())
            }
            assertEquals(result.status, HttpStatusCode.BadRequest)
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByJwtToken(jwt)!!
            val pictureFromDb = usersRepository.getPictureById(id)
            assertNotEquals(validImage.readBytes(), pictureFromDb)
        }
    }

    @Test
    fun `body not a byte array`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingPOST()
            }
            startApplication()
            val db = TestDatabase.apply {
                create()
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
package user.userInfo.get

import common.JwtTokenImpl
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GetPictureByIdTest {
    @Test
    fun `valid request, no profile picture`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val (user, jwt) = createDummyUser(InsertUserPayload("exampleLogin2", "examplePass2"))
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByLogin(user.login)
            val request = client.get("/get-picture-by-id") {
                parameter("id", id)
                parameter("jwtToken", jwt)
                accept(ContentType.Application.Json)
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
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val (user, jwt) = createDummyUser(
                InsertUserPayload(
                    "exampleLogin3",
                    "examplePass3",
                    profilePicture = this.javaClass.getResource("/valid.png")!!.readBytes()
                )
            )
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByLogin(user.login)
            val request = client.get("/get-picture-by-id") {
                parameter("id", id)
                parameter("jwtToken", jwt)
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
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val (user, jwt) = createDummyUser()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByLogin(user.login)!!
            val request = client.get("/get-picture-by-id") {
                this.parameter("id", id + 100L)
                this.parameter("jwtToken", jwt)
            }
            assertEquals(request.status, HttpStatusCode.Forbidden)
        }
    }

    @Test
    fun `no id`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val (user, jwt) = createDummyUser()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            usersRepository.getIdByLogin(user.login)!!
            val request = client.get("/get-picture-by-id") {
                this.parameter("jwtToken", jwt)
            }
            assertEquals(request.status, HttpStatusCode.BadRequest)
        }
    }

    @Test
    fun `id not a long`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val (user, _) = createDummyUser()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            usersRepository.getIdByLogin(user.login)
            val request = client.get("/get-picture-by-id") {
                this.parameter("id", "notLong")
                this.parameter("jwtToken", JwtTokenImpl(user.login + "notValid", user.password).token)
            }
            assertEquals(request.status, HttpStatusCode.Forbidden)
        }
    }

    @Test
    fun `invalid jwt token`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val (user, _) = createDummyUser()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
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
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val (user, _) = createDummyUser()
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByLogin(user.login)
            val request = client.get("/get-picture-by-id") {
                this.parameter("id", id)
            }
            assertEquals(request.status, HttpStatusCode.BadRequest)
        }
    }
}
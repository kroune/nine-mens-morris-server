package user.userInfo.get

import common.JwtTokenImpl
import io.github.kroune.createDummyUser
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext
import user.applyUserPlugins
import user.controller.userinfo.get.userInfoRoutingGET
import user.startTestDI
import userApi.data.dao.UsersDataServiceI
import userApi.model.InsertUserPayload
import kotlin.test.Test
import kotlin.test.assertEquals

class GetRatingByIdTest {
    @Test
    fun `valid request`() {
        testApplication {
            application {
                startTestDI()
                applyUserPlugins()
            }
            routing {
                userInfoRoutingGET()
            }
            startApplication()
            val (user, jwt) = createDummyUser(InsertUserPayload("exampleLogin", "examplePass", rating = 3307))
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByLogin(user.login)
            val request = client.get("/get-rating-by-id") {
                parameter("id", id)
                parameter("jwtToken", jwt)
            }
            assertEquals(request.status, HttpStatusCode.OK)
            assertEquals(Json.decodeFromString<Int>(request.bodyAsText()), user.rating)
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
            val request = client.get("/get-rating-by-id") {
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
            val (_, jwt) = createDummyUser()
            val request = client.get("/get-rating-by-id") {
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
            val request = client.get("/get-rating-by-id") {
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
            val request = client.get("/get-rating-by-id") {
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
            val request = client.get("/get-rating-by-id") {
                this.parameter("id", id)
            }
            assertEquals(request.status, HttpStatusCode.BadRequest)
        }
    }
}
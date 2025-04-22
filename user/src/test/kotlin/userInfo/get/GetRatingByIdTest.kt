package userInfo.get

import common.JwtTokenImpl
import user.data.InsertUserPayload
import user.data.dao.UsersDataServiceI
import commonTests.TestDatabase
import user.applyUserPlugins
import io.github.kroune.createDummyUser
import user.routing.userinfo.get.userInfoRoutingGET
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext
import startTestDI
import kotlin.test.Test

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
            val db = TestDatabase.apply {
                create()
            }
            val (user, jwt) = db.createDummyUser(InsertUserPayload("exampleLogin", "examplePass", rating = 3307))
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
            val db = TestDatabase.apply {
                create()
            }
            val (user, jwt) = db.createDummyUser()
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
            val db = TestDatabase.apply {
                create()
            }
            val (_, jwt) = db.createDummyUser()
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
            val db = TestDatabase.apply {
                create()
            }
            val (user, _) = db.createDummyUser()
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
            val db = TestDatabase.apply {
                create()
            }
            val (user, _) = db.createDummyUser()
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
            val db = TestDatabase.apply {
                create()
            }
            val (user, _) = db.createDummyUser()
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
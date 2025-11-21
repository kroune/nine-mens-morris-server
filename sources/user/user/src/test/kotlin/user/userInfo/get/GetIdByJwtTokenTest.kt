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

class GetIdByJwtTokenTest {

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
            val (user, jwt) = createDummyUser(InsertUserPayload("exampleLogin1", "examplePass1", rating = 3307))
            val koin = GlobalContext.get()
            val usersRepository by koin.inject<UsersDataServiceI>()
            val id = usersRepository.getIdByLogin(user.login)
            val request = client.get("/get-id-by-jwt-token") {
                parameter("jwtToken", jwt)
            }
            assertEquals(request.status, HttpStatusCode.OK)
            assertEquals(Json.decodeFromString<Long>(request.bodyAsText()), id)
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
            usersRepository.getIdByLogin(user.login)
            val request = client.get("/get-id-by-jwt-token") {
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
            usersRepository.getIdByLogin(user.login)
            val request = client.get("/get-id-by-jwt-token")
            assertEquals(request.status, HttpStatusCode.BadRequest)
        }
    }
}
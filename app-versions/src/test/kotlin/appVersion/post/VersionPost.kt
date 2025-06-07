package appVersion.post

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import routing.version.post.versionRoutingPOST
import startTestDI
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionPostTest {
    @Test
    fun `valid request`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingPOST()
        }
        startApplication()
        val response = client.post("/update-version") {
            url {
                parameters.append("version", "5")
                parameters.append("distribution", "Android")
                parameters.append("breaking_changes", "true")
                parameters.append("token", "someSecretToken")
            }
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `invalid token`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingPOST()
        }
        startApplication()
        val response = client.post("/update-version") {
            url {
                parameters.append("version", "5")
                parameters.append("distribution", "Android")
                parameters.append("breaking_changes", "true")
                parameters.append("token", "someNOTSecretToken")
            }
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `missing token parameter`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingPOST()
        }
        startApplication()
        val response = client.post("/update-version") {
            url {
                parameters.append("version", "5")
                parameters.append("distribution", "Android")
                parameters.append("breaking_changes", "true")
            }
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("no [token] parameter found", response.bodyAsText())
    }

    @Test
    fun `missing version parameter`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingPOST()
        }
        startApplication()
        val response = client.post("/update-version") {
            url {
                parameters.append("distribution", "Android")
                parameters.append("breaking_changes", "true")
                parameters.append("token", "someSecretToken")
            }
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("no [version] parameter found", response.bodyAsText())
    }

    @Test
    fun `invalid version format`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingPOST()
        }
        startApplication()
        val response = client.post("/update-version") {
            url {
                parameters.append("version", "not_a_number")
                parameters.append("distribution", "Android")
                parameters.append("breaking_changes", "true")
                parameters.append("token", "someSecretToken")
            }
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("[version] parameter is not valid", response.bodyAsText())
    }

    @Test
    fun `missing distribution parameter`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingPOST()
        }
        startApplication()
        val response = client.post("/update-version") {
            url {
                parameters.append("version", "5")
                parameters.append("breaking_changes", "true")
                parameters.append("token", "someSecretToken")
            }
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("no [distribution] parameter found", response.bodyAsText())
    }

    @Test
    fun `invalid distribution value`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingPOST()
        }
        startApplication()
        val response = client.post("/update-version") {
            url {
                parameters.append("version", "5")
                parameters.append("distribution", "InvalidOS")
                parameters.append("breaking_changes", "true")
                parameters.append("token", "someSecretToken")
            }
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("[distribution] parameter is not valid", response.bodyAsText())
    }

    @Test
    fun `missing breaking_changes parameter`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingPOST()
        }
        startApplication()
        val response = client.post("/update-version") {
            url {
                parameters.append("version", "5")
                parameters.append("distribution", "Ios")
                parameters.append("token", "someSecretToken")
            }
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("no [breaking_changes] parameter found", response.bodyAsText())
    }

    @Test
    fun `invalid breaking_changes format`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingPOST()
        }
        startApplication()
        val response = client.post("/update-version") {
            url {
                parameters.append("version", "5")
                parameters.append("distribution", "Desktop")
                parameters.append("breaking_changes", "not_a_boolean")
                parameters.append("token", "someSecretToken")
            }
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("[breaking_changes] parameter is not valid", response.bodyAsText())
    }
}
package appVersion.get

import appVersion.startTestDI
import appVersions.data.dao.VersionDataServiceI
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import appVersions.model.Distribution
import org.koin.core.context.GlobalContext
import appVersions.version.get.versionRoutingGET
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionGetTest {
    @Test
    fun `get last-version valid request`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingGET()
        }
        startApplication()
        // Add test data
        val koin = GlobalContext.get()
        val versionService = koin.get<VersionDataServiceI>()
        versionService.addVersion(10, Distribution.WasmJs, false)

        val response = client.get("/last-version?distribution=WasmJs")
        assertEquals(HttpStatusCode.OK, response.status)
        val version = Json.decodeFromString<Int>(response.bodyAsText())
        assertEquals(10, version)
    }
    @Test
    fun `get last-version valid request with multiple versions`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingGET()
        }
        startApplication()
        // Add test data
        val koin = GlobalContext.get()
        val versionService = koin.get<VersionDataServiceI>()
        versionService.addVersion(10, Distribution.WasmJs, false)
        versionService.addVersion(21, Distribution.WasmJs, false)

        val response = client.get("/last-version?distribution=WasmJs")
        assertEquals(HttpStatusCode.OK, response.status)
        val version = Json.decodeFromString<Int>(response.bodyAsText())
        assertEquals(21, version)
    }

    @Test
    fun `get last-version missing distribution`() = testApplication {
        application {
            startTestDI()
            routing {
                versionRoutingGET()
            }
        }
        val response = client.get("/last-version")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("no [distribution] parameter found", response.bodyAsText())
    }

    @Test
    fun `get last-version invalid distribution`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingGET()
        }
        startApplication()
        val response = client.get("/last-version?distribution=InvalidOS")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("[distribution] parameter is not valid", response.bodyAsText())
    }

    @Test
    fun `get required-version valid request`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingGET()
        }
        startApplication()
        // Add test data
        val koin = GlobalContext.get()
        val versionService = koin.get<VersionDataServiceI>()
        versionService.addVersion(5, Distribution.IosArm64, false)
        versionService.addVersion(6, Distribution.IosArm64, true)

        val response = client.get("/required-version?version=5&distribution=IosArm64")
        assertEquals(HttpStatusCode.OK, response.status)
        val requiredVersion = Json.decodeFromString<Int>(response.bodyAsText())
        assertEquals(6, requiredVersion)
    }

    @Test
    fun `get required-version returns null`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingGET()
        }
        startApplication()
        // Add test data
        val koin = GlobalContext.get()
        val versionService = koin.get<VersionDataServiceI>()
        versionService.addVersion(5, Distribution.Android, false)

        val response = client.get("/required-version?version=5&distribution=Android")
        assertEquals(HttpStatusCode.OK, response.status)
        val requiredVersion = Json.decodeFromString<Int?>(response.bodyAsText())
        assertEquals(null, requiredVersion)
    }

    @Test
    fun `get required-version missing version`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingGET()
        }
        startApplication()
        val response = client.get("/required-version?distribution=Android")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("no [version] parameter found", response.bodyAsText())
    }

    @Test
    fun `get required-version invalid version format`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingGET()
        }
        startApplication()
        val response = client.get("/required-version?version=invalid&distribution=Android")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("[version] parameter is not valid", response.bodyAsText())
    }

    @Test
    fun `get required-version missing distribution`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingGET()
        }
        startApplication()
        val response = client.get("/required-version?version=2")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("no [distribution] parameter found", response.bodyAsText())
    }

    @Test
    fun `get required-version invalid distribution`() = testApplication {
        application {
            startTestDI()
        }
        routing {
            versionRoutingGET()
        }
        startApplication()
        val response = client.get("/required-version?version=2&distribution=InvalidDistro")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("[distribution] parameter is not valid", response.bodyAsText())
    }
}
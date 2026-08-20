package es

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import java.nio.file.Files
import es.afinavila.module

class ApplicationTest {

    @Test
    fun testBuild() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun legacyPublicDocumentRoutesAreDisabledByDefault() = testApplication {
        val db = Files.createTempFile("afinavila-test-", ".db").toFile()
        application { module(db.absolutePath) }

        assertEquals(HttpStatusCode.Gone, client.get("/comunidad/example").status)
        assertEquals(HttpStatusCode.Gone, client.get("/archivos/example").status)
        assertEquals(HttpStatusCode.Gone, client.get("/archivo/pdf/example/1").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/archivos/session").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/archivo/pdf/session/1").status)
    }

}

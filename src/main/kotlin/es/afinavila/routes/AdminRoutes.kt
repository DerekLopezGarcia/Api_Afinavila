package es.afinavila.routes

import es.afinavila.models.ArchivoTable
import es.afinavila.models.ComunidadTable
import es.afinavila.services.ArchivoService
import es.afinavila.services.ComunidadService
import es.afinavila.services.PasswordVerifier
import es.afinavila.services.RequestSecurity
import es.afinavila.services.LoginRateLimiter
import es.afinavila.services.SessionManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.adminRoutes() {
    val secureCookies = System.getenv("COOKIE_SECURE")?.toBooleanStrictOrNull() ?: true
    val cookieExtensions = mapOf("SameSite" to "Strict")
    post("/admin/login") {
        if (!RequestSecurity.sameOrigin(call)) {
            return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Origen no permitido"))
        }
        val ip = call.request.headers["X-Real-IP"]?.trim()
            ?: call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
            ?: call.request.local.remoteHost

        if (!LoginRateLimiter.tryAcquire(ip)) {
            return@post call.respond(
                HttpStatusCode.TooManyRequests,
                mapOf("error" to "Demasiados intentos. Espere un minuto.")
            )
        }

        val body = runCatching { call.receive<Map<String, String>>() }.getOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Petición inválida"))

        val password = body["password"] ?: ""
        if (!PasswordVerifier.matches(password)) {
            return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Contraseña incorrecta"))
        }

        val token = SessionManager.createAdminSession()

        call.response.cookies.append(
            Cookie(
                name = "afinavila_admin_token",
                value = token,
                httpOnly = true,
                secure = secureCookies,
                path = "/",
                maxAge = 3600,
                extensions = cookieExtensions
            )
        )

        call.respond(mapOf("status" to "ok", "role" to "admin"))
    }

    post("/admin/logout") {
        if (!RequestSecurity.sameOrigin(call)) {
            return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Origen no permitido"))
        }
        call.request.cookies["afinavila_admin_token"]?.let(SessionManager::removeAdmin)
        call.response.cookies.append(
            Cookie("afinavila_admin_token", "", httpOnly = true, secure = secureCookies,
                path = "/", maxAge = 0, extensions = cookieExtensions)
        )
        call.respond(mapOf("status" to "ok"))
    }

    get("/admin/me") {
        val token = call.request.cookies["afinavila_admin_token"]
        if (token == null || !SessionManager.validateAdmin(token)) {
            return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autenticado"))
        }
        call.respond(mapOf("role" to "admin"))
    }

    get("/admin/comunidades") {
        val token = call.request.cookies["afinavila_admin_token"]
        if (token == null || !SessionManager.validateAdmin(token)) {
            return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autenticado"))
        }

        val comunidades = transaction {
            ComunidadTable.selectAll()
                .orderBy(ComunidadTable.numeroComunidad)
                .map { row ->
                    val id = row[ComunidadTable.id].value
                    val totalArchivos = ArchivoTable.select { ArchivoTable.comunidadId eq id }.count()

                    val ultimoArchivo = ArchivoTable
                        .select { ArchivoTable.comunidadId eq id }
                        .orderBy(ArchivoTable.fecha to SortOrder.DESC_NULLS_LAST, ArchivoTable.id to SortOrder.DESC)
                        .limit(1)
                        .firstOrNull()

                    @Suppress("UNCHECKED_CAST")
                    val                     data: Map<String, Any> = mapOf(
                        "id" to id,
                        "nombre" to row[ComunidadTable.nombre],
                        "numeroComunidad" to row[ComunidadTable.numeroComunidad],
                        "claveAcceso" to row[ComunidadTable.claveAcceso],
                        "codigoAcceso" to row[ComunidadTable.codigoAcceso],
                        "totalArchivos" to totalArchivos.toInt(),
                        "ultimoArchivo" to (ultimoArchivo?.get(ArchivoTable.nombreMostrar) ?: ""),
                        "ultimaFecha" to (ultimoArchivo?.get(ArchivoTable.fecha) ?: "")
                    )
                    data
                }
        }

        call.respond(comunidades)
    }

    get("/admin/comunidad/{id}") {
        val token = call.request.cookies["afinavila_admin_token"]
        if (token == null || !SessionManager.validateAdmin(token)) {
            return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autenticado"))
        }

        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
        val comunidad = transaction {
            ComunidadTable.select { ComunidadTable.id eq id }.firstOrNull()
        } ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))

        val archivos = ArchivoService.findByComunidad(comunidad[ComunidadTable.id].value)

        call.respond(mapOf(
            "id" to comunidad[ComunidadTable.id].value,
            "nombre" to comunidad[ComunidadTable.nombre],
            "claveAcceso" to comunidad[ComunidadTable.claveAcceso],
            "codigoAcceso" to comunidad[ComunidadTable.codigoAcceso],
            "archivos" to archivos
        ))
    }

    get("/admin/comunidad/{comunidadId}/archivo/pdf/{id}") {
        val token = call.request.cookies["afinavila_admin_token"]
        if (token == null || !SessionManager.validateAdmin(token)) {
            return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autenticado"))
        }
        val comunidadId = call.parameters["comunidadId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID de comunidad inválido"))
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
        val comunidad = ComunidadService.findById(comunidadId)
            ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))
        val file = ArchivoService.getPdfFileByCodigo(comunidad.codigoAcceso, id)
            ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Archivo no encontrado"))
        val safeDownloadName = file.name.replace(Regex("[^a-zA-Z0-9._ -]"), "_")
        call.response.header("Content-Type", "application/pdf")
        call.response.header("Content-Disposition", "inline; filename=\"$safeDownloadName\"")
        call.respondFile(file)
    }
}

package es.afinavila.routes

import es.afinavila.models.ComunidadTable
import es.afinavila.models.ArchivoTable
import es.afinavila.services.LoginRateLimiter
import es.afinavila.services.SessionManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private const val ADMIN_PASSWORD = "482687"

fun Route.adminRoutes() {
    post("/admin/login") {
        val ip = call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
            ?: call.request.headers["X-Real-IP"]
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
        if (password != ADMIN_PASSWORD) {
            return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Contraseña incorrecta"))
        }

        val token = SessionManager.createAdminSession()

        call.response.cookies.append(
            Cookie(
                name = "afinavila_admin_token",
                value = token,
                httpOnly = true,
                secure = false,
                path = "/",
                maxAge = 3600
            )
        )

        call.respond(mapOf("status" to "ok", "role" to "admin"))
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
                .orderBy(ComunidadTable.nombre)
                .map { row ->
                    val id = row[ComunidadTable.id].value
                    val totalArchivos = ArchivoTable.select { ArchivoTable.comunidadId eq id }.count()

                    val ultimoArchivo = ArchivoTable
                        .select { ArchivoTable.comunidadId eq id }
                        .orderBy(ArchivoTable.fecha to SortOrder.DESC_NULLS_LAST, ArchivoTable.id to SortOrder.DESC)
                        .limit(1)
                        .firstOrNull()

                    @Suppress("UNCHECKED_CAST")
                    val data: Map<String, Any> = mapOf(
                        "id" to id,
                        "nombre" to row[ComunidadTable.nombre],
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

    get("/admin/comunidad/{codigoAcceso}") {
        val token = call.request.cookies["afinavila_admin_token"]
        if (token == null || !SessionManager.validateAdmin(token)) {
            return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autenticado"))
        }

        val codigo = call.parameters["codigoAcceso"] ?: ""
        if (codigo.isEmpty()) {
            return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Código inválido"))
        }

        val comunidad = transaction {
            ComunidadTable.select { ComunidadTable.codigoAcceso eq codigo }
                .firstOrNull()
        } ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))

        val archivos = transaction {
            ArchivoTable.select { ArchivoTable.comunidadId eq comunidad[ComunidadTable.id].value }
                .orderBy(ArchivoTable.fecha to SortOrder.DESC_NULLS_LAST, ArchivoTable.nombreMostrar to SortOrder.ASC)
                .map { row ->
                    mapOf(
                        "id" to row[ArchivoTable.id].value,
                        "nombre" to row[ArchivoTable.nombre],
                        "nombreMostrar" to row[ArchivoTable.nombreMostrar],
                        "descripcion" to row[ArchivoTable.descripcion],
                        "fecha" to (row[ArchivoTable.fecha] ?: "")
                    )
                }
        }

        call.respond(mapOf(
            "id" to comunidad[ComunidadTable.id].value,
            "nombre" to comunidad[ComunidadTable.nombre],
            "claveAcceso" to comunidad[ComunidadTable.claveAcceso],
            "codigoAcceso" to comunidad[ComunidadTable.codigoAcceso],
            "archivos" to archivos
        ))
    }
}

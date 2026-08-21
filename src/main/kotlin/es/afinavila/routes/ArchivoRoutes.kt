package es.afinavila.routes

import es.afinavila.models.ComunidadResponse
import es.afinavila.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.archivoRoutes() {

    val allowLegacyPublicAccess = System.getenv("ALLOW_LEGACY_PUBLIC_ACCESS")?.toBooleanStrictOrNull() == true
    val secureCookies = System.getenv("COOKIE_SECURE")?.toBooleanStrictOrNull() ?: true
    val cookieExtensions = mapOf("SameSite" to "Lax")

    get("/health") {
        call.respond(mapOf("status" to "ok"))
    }

    post("/auth/login") {
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

        val request = runCatching { call.receive<LoginClientRequest>() }.getOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Petición inválida"))
        val code = request.codigoAcceso.trim()
        if (code.isEmpty() || code.length > 20 || !code.matches(Regex("^[a-zA-Z0-9]+$"))) {
            return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Código incorrecto"))
        }
        val comunidad = ComunidadService.findByClaveAcceso(code)
            ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Código incorrecto"))

        val token = SessionManager.create(comunidad.codigoAcceso, comunidad.id, comunidad.nombre)

        call.response.cookies.append(
            Cookie(
                name = "afinavila_token",
                value = token,
                httpOnly = true,
                secure = secureCookies,
                path = "/api/",
                maxAge = 3600,
                extensions = cookieExtensions
            )
        )

        call.respond(mapOf(
            "id" to comunidad.id,
            "nombre" to comunidad.nombre
        ))
    }

    post("/auth/logout") {
        if (!RequestSecurity.sameOrigin(call)) {
            return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Origen no permitido"))
        }
        call.request.cookies["afinavila_token"]?.let(SessionManager::remove)
        call.response.cookies.append(
            Cookie("afinavila_token", "", httpOnly = true, secure = secureCookies,
                path = "/api/", maxAge = 0, extensions = cookieExtensions)
        )
        call.respond(mapOf("status" to "ok"))
    }

    get("/auth/me") {
        val token = call.request.cookies["afinavila_token"]
        if (token == null) {
            return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autenticado"))
        }
        val session = SessionManager.validate(token)
            ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Sesión expirada"))
        call.respond(ComunidadResponse(session.comunidadId, session.comunidadNombre, "", "", ""))
    }

    get("/comunidad/{codigoAcceso}") {
        if (!allowLegacyPublicAccess) {
            return@get call.respond(HttpStatusCode.Gone, mapOf("error" to "Endpoint retirado"))
        }
        val codigo = call.parameters["codigoAcceso"] ?: ""
        if (codigo.isEmpty() || !codigo.matches(Regex("^[a-zA-Z0-9]+$"))) {
            return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Código inválido"))
        }
        val comunidad = ComunidadService.findByClaveAcceso(codigo)
            ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))
        call.respond(comunidad)
    }

    get("/archivos/{codigoAcceso}") {
        if (!allowLegacyPublicAccess) {
            return@get call.respond(HttpStatusCode.Gone, mapOf("error" to "Endpoint retirado"))
        }
        val codigo = call.parameters["codigoAcceso"] ?: ""
        if (codigo.isEmpty() || !codigo.matches(Regex("^[a-zA-Z0-9]+$"))) {
            return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Código inválido"))
        }
        val comunidad = ComunidadService.findByClaveAcceso(codigo)
            ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))
        val archivos = ArchivoService.findByComunidad(comunidad.id)
        call.respond(archivos)
    }

    get("/archivo/pdf/{codigoAcceso}/{id}") {
        if (!allowLegacyPublicAccess) {
            val requestedCode = call.parameters["codigoAcceso"] ?: ""
            val clientSession = call.request.cookies["afinavila_token"]?.let(SessionManager::validate)
            val adminSession = call.request.cookies["afinavila_admin_token"]
                ?.let(SessionManager::validateAdmin) == true
            val requestedCommunity = ComunidadService.findByClaveAcceso(requestedCode)
                ?: ComunidadService.findByCodigoAcceso(requestedCode)
            val authorizedCompatibility = adminSession ||
                (clientSession != null && requestedCommunity?.id == clientSession.comunidadId)
            if (!authorizedCompatibility) {
                return@get call.respond(HttpStatusCode.Gone, mapOf("error" to "Endpoint retirado"))
            }
        }
        val codigo = call.parameters["codigoAcceso"] ?: ""
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
        if (codigo.isEmpty() || !codigo.matches(Regex("^[a-zA-Z0-9]+$"))) {
            return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Código inválido"))
        }
        val comunidad = ComunidadService.findByClaveAcceso(codigo)
            ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))
        val file = ArchivoService.getPdfFileByCodigo(comunidad.codigoAcceso, id)
        if (file != null) {
            val safeDownloadName = file.name.replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            call.response.header("Content-Type", "application/pdf")
            call.response.header("Content-Disposition", "inline; filename=\"$safeDownloadName\"")
            call.respondFile(file)
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Archivo no encontrado"))
        }
    }

    // === Session-based endpoints (for web, no codigoAcceso in URL) ===

    get("/archivos/session") {
        val token = call.request.cookies["afinavila_token"]
        if (token == null) {
            return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autenticado"))
        }
        val session = SessionManager.validate(token)
            ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Sesión expirada"))
        val archivos = ArchivoService.findByComunidadCodigo(session.codigoAcceso)
            ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))
        call.respond(archivos)
    }

    get("/archivo/pdf/session/{id}") {
        val token = call.request.cookies["afinavila_token"]
        if (token == null) {
            return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No autenticado"))
        }
        val session = SessionManager.validate(token)
            ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Sesión expirada"))
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
        val file = ArchivoService.getPdfFileByCodigo(session.codigoAcceso, id)
        if (file != null) {
            val safeDownloadName = file.name.replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            call.response.header("Content-Type", "application/pdf")
            call.response.header("Content-Disposition", "inline; filename=\"$safeDownloadName\"")
            call.respondFile(file)
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Archivo no encontrado"))
        }
    }
}

private data class LoginClientRequest(val codigoAcceso: String)

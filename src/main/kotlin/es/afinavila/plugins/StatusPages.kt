package es.afinavila.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Exception> { call, cause ->
            call.application.environment.log.error("Internal error", cause)
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = mapOf("error" to "Error interno del servidor")
            )
        }
    }
}

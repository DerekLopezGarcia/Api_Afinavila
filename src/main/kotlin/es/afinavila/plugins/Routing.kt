package es.afinavila.plugins

import es.afinavila.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        archivoRoutes()
        comunidadRoutes()
        authRoutes()
    }
}

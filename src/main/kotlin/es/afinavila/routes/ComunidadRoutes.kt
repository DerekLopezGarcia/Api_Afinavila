package es.afinavila.routes

import es.afinavila.models.ComunidadRequest
import es.afinavila.services.ComunidadService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.comunidadRoutes() {
    route("/comunidades") {
        get {
            val comunidades = ComunidadService.findAll()
            call.respond(comunidades)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
            val comunidad = ComunidadService.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))
            call.respond(comunidad)
        }

        authenticate("auth-jwt") {
            post {
                val request = call.receive<ComunidadRequest>()
                val comunidad = ComunidadService.create(request)
                call.respond(HttpStatusCode.Created, comunidad)
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val request = call.receive<ComunidadRequest>()
                val updated = ComunidadService.update(id, request)
                if (updated) call.respond(mapOf("message" to "Comunidad actualizada"))
                else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val deleted = ComunidadService.delete(id)
                if (deleted) call.respond(mapOf("message" to "Comunidad eliminada"))
                else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))
            }
        }
    }
}

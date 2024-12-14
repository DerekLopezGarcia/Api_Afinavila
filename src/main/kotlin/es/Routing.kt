package com.afinavila

import es.model.comunidad.Comunidad
import com.afinavila.model.cominidad.ComunidadJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        staticResources("/static", "static")
        get("/") {
            call.respondText("Hola Juan!")
        }
        //Ruta Comunidades
        get("/comunidades") {
            call.respond(ComunidadJson.getComunidad())
        }
        //Ruta Comunidades json
        get("/comunidades/json") {
            call.respondText(ComunidadJson.getComunidadJson())
        }
        // Ruta para agregar Comunidad
        post("/comunidades") {
            val comunidad = call.receive<Comunidad>()
            ComunidadJson.addComunidad(comunidad)
            call.respondText("Comunidad added successfully", status = HttpStatusCode.Created)
        }
        // Ruta para eliminar Comunidad
        delete("/comunidades/{id}") {
            val id: Int = call.parameters["id"]!!.toInt()
            if (ComunidadJson.deleteComunidad(id)) {
                call.respondText("Comunidad deleted successfully", status = HttpStatusCode.OK)
            } else {
                call.respondText("Comunidad not found", status = HttpStatusCode.NotFound)
            }
        }
    }
}

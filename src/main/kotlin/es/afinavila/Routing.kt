package es.afinavila

import es.afinavila.controler.ComunidadControler
import es.afinavila.model.ArchivoDAO
import es.afinavila.model.ArchivoModel
import es.afinavila.model.ComunidadModel
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

fun Application.configureRouting() {
    val comunidadControler = ComunidadControler()
    routing {
        staticResources("/static", "static")
        get("/") {
            call.respondText("Hola Juan!")
        }
        // Ruta Comunidades
        get("/comunidades") {
            call.respond(comunidadControler.getComunidades())
        }
        // Ruta para agregar Comunidad
        post("/comunidades") {
            val comunidadModel = call.receive<ComunidadModel>()
            comunidadControler.addComunidad(comunidadModel)
            call.respondText("Comunidad added successfully", status = HttpStatusCode.Created)
        }
        // Ruta para eliminar Comunidad
        delete("/comunidades/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null && comunidadControler.deleteComunidad(id)) {
                call.respondText("Comunidad deleted successfully", status = HttpStatusCode.OK)
            } else {
                call.respondText("Comunidad not found", status = HttpStatusCode.NotFound)
            }
        }
        put("/comunidades/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val comunidadModel = call.receive<ComunidadModel>()
            if (id != null && comunidadControler.updateComunidad(id, comunidadModel)) {
                call.respondText("Comunidad updated successfully", status = HttpStatusCode.OK)
            } else {
                call.respondText("Comunidad not found", status = HttpStatusCode.NotFound)
            }
        }
        get("/comunidades/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                val comunidad = comunidadControler.getComunidad(id)
                if (comunidad != null) {
                    call.respond(comunidad)
                } else {
                    call.respondText("Comunidad not found", status = HttpStatusCode.NotFound)
                }
            } else {
                call.respondText("Invalid id", status = HttpStatusCode.BadRequest)
            }
        }
        post("/archivo/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                comunidadControler.getComunidad(id)?.let { comunidad ->
                    val multipart = call.receiveMultipart()
                    var descripcion: String? = null
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val directory = File(comunidad.codigoAcceso)
                                val file = File(directory, part.originalFileName!!)
                                ArchivoModel(0, part.originalFileName!!, descripcion ?: "").let {
                                    ArchivoDAO.addArchivo(it)
                                }
                                part.streamProvider().use { input ->
                                    file.outputStream().buffered().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            else -> Unit
                        }
                        part.dispose()
                    }
                    call.respondText("Archivo uploaded successfully", status = HttpStatusCode.Created)
                } ?: call.respondText("Comunidad not found", status = HttpStatusCode.NotFound)
            } else {
                call.respondText("Invalid id", status = HttpStatusCode.BadRequest)
            }
        }
    }
}
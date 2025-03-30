package es.afinavila

import es.afinavila.controler.ArchivoControler
import es.afinavila.controler.ComunidadControler
import es.afinavila.model.ComunidadModel
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.jvm.javaio.copyTo
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import java.io.File

fun Application.configureRouting() {
    val comunidadControler = ComunidadControler()
    val archivoControler = ArchivoControler()
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
        // Ruta para actualizar Comunidad
        put("/comunidades/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val comunidadModel = call.receive<ComunidadModel>()
            if (id != null && comunidadControler.updateComunidad(id, comunidadModel)) {
                call.respondText("Comunidad updated successfully", status = HttpStatusCode.OK)
            } else {
                call.respondText("Comunidad not found", status = HttpStatusCode.NotFound)
            }
        }
        // Ruta para obtener Comunidad
        get("/comunidades/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                archivoControler.updateArchivos(id)
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
        // Ruta para subir archivo
        post("/archivo/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                comunidadControler.getComunidad(id)?.let { comunidad ->
                    val multipart = call.receiveMultipart()
                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val file = File("comunidades/${comunidad.codigoAcceso}", part.originalFileName!!)
                                part.streamProvider().use { inputStream ->
                                    file.outputStream().buffered().use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                archivoControler.addArchivo(id, file)
                            }
                            else -> Unit
                        }
                        part.dispose()
                    }
                    call.respondText(
                        "Archivo uploaded successfully",
                        status = HttpStatusCode.Created
                    )
                } ?: call.respondText("Comunidad not found", status = HttpStatusCode.NotFound)
            } else {
                call.respondText("Invalid id", status = HttpStatusCode.BadRequest)
            }
        }
        // Ruta para eliminar archivo
        delete("/archivo/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                archivoControler.deleteArchivo(id)
                call.respondText("Archivo deleted successfully", status = HttpStatusCode.OK)
            }
        }
        // Ruta para obtener archivo
        get("/archivo/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                val archivo = archivoControler.getArchivo(id)
                if (archivo != null) {
                    call.respond(archivo)
                } else {
                    call.respondText("Archivo not found", status = HttpStatusCode.NotFound)
                }
            } else {
                call.respondText("Invalid id", status = HttpStatusCode.BadRequest)
            }
        }
        // Ruta para obtener tdos los archivos de una comunidad
        get("/archivos/{comunidadId}") {
            val comunidadId = call.parameters["comunidadId"]?.toIntOrNull()
            if (comunidadId != null) {
                val archivos = archivoControler.getArchivosByComunidad(comunidadId)
                call.respond(archivos)
            } else {
                call.respondText("Invalid id", status = HttpStatusCode.BadRequest)
            }
        }
    }
}
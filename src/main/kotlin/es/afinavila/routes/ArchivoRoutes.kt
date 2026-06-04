package es.afinavila.routes

import es.afinavila.models.ArchivoResponse
import es.afinavila.services.ArchivoService
import es.afinavila.services.ComunidadService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Route.archivoRoutes() {
    route("/archivos") {
        get("/{comunidadId}") {
            val comunidadId = call.parameters["comunidadId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID de comunidad invalido"))
            val comunidad = ComunidadService.findById(comunidadId)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))
            val archivos = ArchivoService.findByComunidad(comunidadId)
            call.respond(archivos)
        }
    }

    route("/archivo") {
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID invalido"))
            val archivo = ArchivoService.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Archivo no encontrado"))
            call.respond(archivo)
        }

        get("/pdf/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID invalido"))
            val file = ArchivoService.getPdfFile(id)
            if (file != null) {
                call.response.header("Content-Type", "application/pdf")
                call.response.header("Content-Disposition", "inline; filename=\"${file.name}\"")
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Archivo no encontrado"))
            }
        }

        authenticate("auth-jwt") {
            post("/{comunidadId}") {
                val comunidadId = call.parameters["comunidadId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID invalido"))
                val comunidad = ComunidadService.findById(comunidadId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comunidad no encontrada"))

                val multipart = call.receiveMultipart()
                val uploaded = mutableListOf<ArchivoResponse>()
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val dir = File("${ArchivoService.filesPath}/${comunidad.codigoAcceso}")
                        dir.mkdirs()
                        val dest = File(dir, part.originalFileName ?: "archivo.pdf")
                        @Suppress("DEPRECATION")
                        part.streamProvider().use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                        uploaded.add(ArchivoService.addFile(comunidad, dest.name))
                    }
                    part.dispose()
                }
                if (uploaded.isNotEmpty()) {
                    call.respond(HttpStatusCode.Created, uploaded)
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No se recibio ningun archivo"))
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID invalido"))
                val deleted = ArchivoService.delete(id)
                if (deleted) call.respond(mapOf("message" to "Archivo eliminado"))
                else call.respond(HttpStatusCode.NotFound, mapOf("error" to "Archivo no encontrado"))
            }
        }
    }
}

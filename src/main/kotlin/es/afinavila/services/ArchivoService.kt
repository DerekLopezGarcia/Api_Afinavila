package es.afinavila.services

import es.afinavila.models.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object ArchivoService {
    var filesPath: String = "comunidades"

    fun findByComunidad(comunidadId: Int): List<ArchivoResponse> = transaction {
        ArchivoTable.select { ArchivoTable.comunidadId eq comunidadId }
            .orderBy(ArchivoTable.fecha to SortOrder.DESC_NULLS_LAST, ArchivoTable.nombreMostrar to SortOrder.ASC)
            .map { it.toResponse() }
    }

    fun findById(id: Int): ArchivoResponse? = transaction {
        ArchivoTable.select { ArchivoTable.id eq id }.singleOrNull()?.toResponse()
    }

    fun findByComunidadCodigo(codigoAcceso: String): List<ArchivoResponse>? {
        val comunidad = ComunidadService.findByCodigoAcceso(codigoAcceso)
            ?: ComunidadService.findByClaveAcceso(codigoAcceso)
            ?: return null
        return findByComunidad(comunidad.id)
    }

    fun getPdfFileByCodigo(codigoAcceso: String, archivoId: Int): File? {
        val archivo = findById(archivoId) ?: return null
        val comunidad = ComunidadService.findByCodigoAcceso(codigoAcceso)
            ?: ComunidadService.findByClaveAcceso(codigoAcceso)
            ?: return null
        if (archivo.comunidadId != comunidad.id) return null
        val safeName = sanitizePath(archivo.nombre)
        val safeCodigo = sanitizePath(comunidad.codigoAcceso)
        if (safeCodigo.isEmpty() || safeName.isEmpty()) return null
        val file = File("$filesPath/$safeCodigo/$safeName")
        val canonicalRoot = File(filesPath).canonicalPath
        val canonicalFile = file.canonicalFile
        if (!canonicalFile.path.startsWith(canonicalRoot)) return null
        return if (file.exists()) file else null
    }

    fun addFile(comunidad: ComunidadResponse, filename: String): ArchivoResponse {
        val parsed = FileNameParser.parse(filename)
        return transaction {
            val existing = ArchivoTable.select {
                (ArchivoTable.comunidadId eq comunidad.id) and (ArchivoTable.nombre eq filename)
            }.singleOrNull()
            if (existing != null) return@transaction existing.toResponse()

            val newId: EntityID<Int> = ArchivoTable.insertAndGetId {
                it[nombre] = filename
                it[nombreMostrar] = parsed.nombreMostrar
                it[descripcion] = parsed.descripcion
                it[fecha] = parsed.fecha
                it[comunidadId] = comunidad.id
            }
            ArchivoResponse(newId.value, filename, parsed.nombreMostrar, parsed.descripcion, comunidad.id, parsed.categoria, parsed.fecha)
        }
    }

    fun deleteFilesNotIn(comunidadId: Int, nombresEnDisco: Set<String>) {
        transaction {
            ArchivoTable.deleteWhere {
                (ArchivoTable.comunidadId eq comunidadId) and (ArchivoTable.nombre notInList nombresEnDisco.toList())
            }
        }
    }

    fun syncFromDisk() {
        val rootDir = File(filesPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return

        val newFormat = Regex("^\\d{1,2} [a-zA-Z0-9]+$")
        val oldFormat = Regex("^[a-zA-Z0-9]+$")

        // 1. Recolectar todos los IDs de comunidad que existen en disco
        val comunidadesEnDisco = mutableSetOf<Int>()

        rootDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val folderName = dir.name
            val (numero, clave) = when {
                folderName.matches(newFormat) -> {
                    val parts = folderName.split(" ", limit = 2)
                    parts[0] to parts[1]
                }
                folderName.matches(oldFormat) -> {
                    "" to folderName
                }
                else -> return@forEach
            }

            val codigoAcceso = if (numero.isNotEmpty()) "$numero $clave" else clave
            var comunidad = ComunidadService.findByCodigoAcceso(codigoAcceso)
                ?: ComunidadService.findByClaveAcceso(clave)

            if (comunidad == null) {
                val nombreReal = ComunidadNames.getName(clave)
                    .let { if (it == clave) ComunidadNames.getName(folderName) else it }
                    .let { if (it == folderName) clave else it }
                comunidad = ComunidadService.create(
                    ComunidadRequest(
                        nombre = nombreReal,
                        numeroComunidad = numero.ifEmpty { "00" },
                        claveAcceso = clave
                    )
                )
            } else {
                val nombreReal = ComunidadNames.getName(clave)
                    .let { if (it == clave) ComunidadNames.getName(folderName) else it }
                if (nombreReal != comunidad.nombre && nombreReal != clave && nombreReal != folderName) {
                    ComunidadService.updateNombre(comunidad.id, nombreReal)
                    comunidad = ComunidadService.findById(comunidad.id)
                }
            }

            comunidadesEnDisco.add(comunidad!!.id)

            // 2. Sincronizar archivos de esta comunidad
            val pdfsEnDisco = dir.listFiles()?.filter {
                !it.isDirectory && it.name.lowercase().endsWith(".pdf") &&
                    it.name.matches(Regex("^[a-zA-Z0-9._\\- ()]+\\.pdf\$"))
            }?.map { it.name }?.toSet() ?: emptySet()

            // Añadir archivos nuevos
            pdfsEnDisco.forEach { filename ->
                addFile(comunidad!!, filename)
            }

            // Eliminar archivos que ya no están en disco
            deleteFilesNotIn(comunidad!!.id, pdfsEnDisco)
        }

        // 3. Eliminar comunidades cuyas carpetas ya no existen en disco
        val todasLasComunidades = transaction {
            ComunidadTable.selectAll().map { it[ComunidadTable.id].value }
        }
        val comunidadesAEliminar = todasLasComunidades - comunidadesEnDisco
        if (comunidadesAEliminar.isNotEmpty()) {
            val entityIds = comunidadesAEliminar.map { EntityID(it, ComunidadTable) }
            transaction {
                // Primero eliminar archivos de esas comunidades
                ArchivoTable.deleteWhere { ArchivoTable.comunidadId inList comunidadesAEliminar }
                // Luego eliminar comunidades (id es EntityID<Int>, no Int directo)
                ComunidadTable.deleteWhere { ComunidadTable.id inList entityIds }
            }
        }
    }

    private fun sanitizePath(segment: String): String = segment.replace(Regex("[^a-zA-Z0-9._\\- ]"), "")

    private fun ResultRow.toResponse() = ArchivoResponse(
        id = this[ArchivoTable.id].value,
        nombre = this[ArchivoTable.nombre],
        nombreMostrar = this[ArchivoTable.nombreMostrar],
        descripcion = this[ArchivoTable.descripcion],
        comunidadId = this[ArchivoTable.comunidadId],
        categoria = FileNameParser.parse(this[ArchivoTable.nombre]).categoria,
        fecha = this[ArchivoTable.fecha]
    )
}

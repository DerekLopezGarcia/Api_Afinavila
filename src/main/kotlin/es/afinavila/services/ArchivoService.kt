package es.afinavila.services

import es.afinavila.models.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object ArchivoService {
    var filesPath: String = "comunidades"

    fun findByComunidad(comunidadId: Int): List<ArchivoResponse> = transaction {
        ArchivoTable.select { ArchivoTable.comunidadId eq comunidadId }
            .orderBy(ArchivoTable.nombreMostrar)
            .map { it.toResponse() }
    }

    fun findById(id: Int): ArchivoResponse? = transaction {
        ArchivoTable.select { ArchivoTable.id eq id }.singleOrNull()?.toResponse()
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
                it[comunidadId] = comunidad.id
            }
            ArchivoResponse(newId.value, filename, parsed.nombreMostrar, parsed.descripcion, comunidad.id, parsed.categoria)
        }
    }

    fun delete(id: Int): Boolean = transaction {
        val archivo = ArchivoTable.select { ArchivoTable.id eq id }.singleOrNull() ?: return@transaction false
        val comunidad = ComunidadTable.select { ComunidadTable.id eq archivo[ArchivoTable.comunidadId] }.singleOrNull()
        if (comunidad != null) {
            val file = File("$filesPath/${comunidad[ComunidadTable.codigoAcceso]}/${archivo[ArchivoTable.nombre]}")
            if (file.exists()) file.delete()
        }
        ArchivoTable.deleteWhere { ArchivoTable.id eq id } > 0
    }

    fun getPdfFile(id: Int): File? {
        val archivo = findById(id) ?: return null
        val comunidad = ComunidadService.findById(archivo.comunidadId) ?: return null
        val file = File("$filesPath/${comunidad.codigoAcceso}/${archivo.nombre}")
        return if (file.exists()) file else null
    }

    fun syncFromDisk() {
        val rootDir = File(filesPath)
        if (!rootDir.exists() || !rootDir.isDirectory) return

        rootDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val codigoAcceso = dir.name
            var comunidad = ComunidadService.findByCodigoAcceso(codigoAcceso)
            if (comunidad == null) {
                comunidad = ComunidadService.create(ComunidadRequest(codigoAcceso, codigoAcceso))
            }
            dir.listFiles()?.filter { !it.isDirectory && it.name.lowercase().endsWith(".pdf") }?.forEach { file ->
                addFile(comunidad!!, file.name)
            }
        }
    }

    private fun ResultRow.toResponse() = ArchivoResponse(
        id = this[ArchivoTable.id].value,
        nombre = this[ArchivoTable.nombre],
        nombreMostrar = this[ArchivoTable.nombreMostrar],
        descripcion = this[ArchivoTable.descripcion],
        comunidadId = this[ArchivoTable.comunidadId],
        categoria = FileNameParser.parse(this[ArchivoTable.nombre]).categoria
    )
}

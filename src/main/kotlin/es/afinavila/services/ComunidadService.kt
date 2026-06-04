package es.afinavila.services

import es.afinavila.models.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object ComunidadService {

    fun findAll(): List<ComunidadResponse> = transaction {
        ComunidadTable.selectAll().orderBy(ComunidadTable.nombre).map { it.toResponse() }
    }

    fun findById(id: Int): ComunidadResponse? = transaction {
        ComunidadTable.select { ComunidadTable.id eq id }.singleOrNull()?.toResponse()
    }

    fun findByCodigoAcceso(code: String): ComunidadResponse? = transaction {
        ComunidadTable.select { ComunidadTable.codigoAcceso eq code }.firstOrNull()?.toResponse()
    }

    fun create(request: ComunidadRequest): ComunidadResponse = transaction {
        val newId: EntityID<Int> = ComunidadTable.insertAndGetId {
            it[nombre] = request.nombre
            it[codigoAcceso] = request.codigoAcceso
        }
        ComunidadResponse(newId.value, request.nombre, request.codigoAcceso)
    }

    fun update(id: Int, request: ComunidadRequest): Boolean = transaction {
        ComunidadTable.update({ ComunidadTable.id eq id }) {
            it[nombre] = request.nombre
            it[codigoAcceso] = request.codigoAcceso
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        val comunidad = ComunidadTable.select { ComunidadTable.id eq id }.singleOrNull() ?: return@transaction false
        val archivos = ArchivoTable.select { ArchivoTable.comunidadId eq id }.toList()
        for (archivo in archivos) {
            val file = File("${ArchivoService.filesPath}/${comunidad[ComunidadTable.codigoAcceso]}/${archivo[ArchivoTable.nombre]}")
            if (file.exists()) file.delete()
        }
        ArchivoTable.deleteWhere { ArchivoTable.comunidadId eq id }
        ComunidadTable.deleteWhere { ComunidadTable.id eq id } > 0
    }

    private fun ResultRow.toResponse() = ComunidadResponse(
        id = this[ComunidadTable.id].value,
        nombre = this[ComunidadTable.nombre],
        codigoAcceso = this[ComunidadTable.codigoAcceso]
    )
}

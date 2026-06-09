package es.afinavila.services

import es.afinavila.models.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object ComunidadService {

    fun findById(id: Int): ComunidadResponse? = transaction {
        ComunidadTable.select { ComunidadTable.id eq id }.singleOrNull()?.toResponse()
    }

    fun findByCodigoAcceso(code: String): ComunidadResponse? = transaction {
        ComunidadTable.select { ComunidadTable.codigoAcceso eq code }.firstOrNull()?.toResponse()
    }

    fun findByClaveAcceso(clave: String): ComunidadResponse? = transaction {
        ComunidadTable.select { ComunidadTable.claveAcceso eq clave }.firstOrNull()?.toResponse()
    }

    fun create(request: ComunidadRequest): ComunidadResponse = transaction {
        val newId: EntityID<Int> = ComunidadTable.insertAndGetId {
            it[nombre] = request.nombre
            it[numeroComunidad] = request.numeroComunidad
            it[claveAcceso] = request.claveAcceso
            it[codigoAcceso] = request.codigoAcceso
        }
        ComunidadResponse(
            id = newId.value,
            nombre = request.nombre,
            numeroComunidad = request.numeroComunidad,
            claveAcceso = request.claveAcceso,
            codigoAcceso = request.codigoAcceso
        )
    }

    fun updateNombre(id: Int, nuevoNombre: String): Boolean = transaction {
        ComunidadTable.update({ ComunidadTable.id eq id }) {
            it[nombre] = nuevoNombre
        } > 0
    }

    private fun ResultRow.toResponse() = ComunidadResponse(
        id = this[ComunidadTable.id].value,
        nombre = this[ComunidadTable.nombre],
        numeroComunidad = this[ComunidadTable.numeroComunidad],
        claveAcceso = this[ComunidadTable.claveAcceso],
        codigoAcceso = this[ComunidadTable.codigoAcceso]
    )
}

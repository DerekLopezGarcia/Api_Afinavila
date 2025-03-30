package es.afinavila.model


import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object Comunidad : IntIdTable() {
    val nombre = varchar("nombre", 100)
    val codacces = varchar("codacces", 20)
}

data class ComunidadModel(
    val id: Int? = null,
    val nombre: String,
    val codigoAcceso: String
)

fun ResultRow.toComunidad() = ComunidadModel(
    id = this[Comunidad.id].value,
    nombre = this[Comunidad.nombre],
    codigoAcceso = this[Comunidad.codacces]
)

object ComunidadDAO {
    fun getComunidades(): List<ComunidadModel> = transaction {
        Comunidad.selectAll().map { it.toComunidad() }
    }

    fun addComunidad(comunidadModel: ComunidadModel) = transaction {
        Comunidad.insert {
            it[nombre] = comunidadModel.nombre
            it[codacces] = comunidadModel.codigoAcceso
        }
    }

    fun deleteComunidad(id: Int): Boolean = transaction {
        Comunidad.deleteWhere { Comunidad.id eq id } > 0
    }
    fun updateComunidad(id: Int, comunidadModel: ComunidadModel) = transaction {
        Comunidad.update({ Comunidad.id eq id }) {
            it[nombre] = comunidadModel.nombre
            it[codacces] = comunidadModel.codigoAcceso
        }
    }
    fun getComunidad(id: Int): ComunidadModel? = transaction {
        Comunidad.select { Comunidad.id eq id }.firstOrNull()?.toComunidad()
    }
    fun getComunidadByCodigoAcceso(codigoAcceso: String): ComunidadModel? = transaction {
        Comunidad.select { Comunidad.codacces eq codigoAcceso }
            .map { it.toComunidad() }
            .singleOrNull()
    }
}
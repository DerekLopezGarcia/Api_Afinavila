package es.afinavila.model

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object Archivo: IntIdTable() {
    val nombre = varchar("nombre", 100)
    val descripcion = varchar("descripcion", 200)
}
data class ArchivoModel(
    val id: Int,
    val nombre: String,
    val descripcion: String
)
fun ResultRow.toArchivo() = ArchivoModel(
    id = this[Archivo.id].value,
    nombre = this[Archivo.nombre],
    descripcion = this[Archivo.descripcion]
)
object ArchivoDAO {
    fun addArchivo(archivoModel: ArchivoModel) = transaction {
        Archivo.insert {
            it[nombre] = archivoModel.nombre
            it[descripcion] = archivoModel.descripcion
        }
    }
    fun deleteArchivo(id: Int): Boolean = transaction {
        Archivo.deleteWhere { Archivo.id eq id } > 0
    }
}
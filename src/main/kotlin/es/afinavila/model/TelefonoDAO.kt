package es.afinavila.model


import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object Telefono: IntIdTable() {
    val nombre = varchar("nombre", 20)
    val telefono = varchar("telefono", 9)
}
data class TelefonoModel(
    val id: Int,
    val nombre: String,
    val telefono: String
)
fun ResultRow.toTelefono() = TelefonoModel(
    id = this[Telefono.id].value,
    nombre = this[Telefono.nombre],
    telefono = this[Telefono.telefono]
)
object TelefonoDAO {
    fun getTelefonos(): List<TelefonoModel> = transaction {
        Telefono.selectAll().map { it.toTelefono() }
    }
    fun addTelefono(telefonoModel: TelefonoModel) = transaction {
        Telefono.insert {
            it[nombre] = telefonoModel.nombre
            it[telefono] = telefonoModel.telefono
        }
    }
    fun deleteTelefono(id: Int): Boolean = transaction {
        Telefono.deleteWhere { Telefono.id eq id } > 0
    }
    fun updateTelefono(id: Int, telefonoModel: TelefonoModel) = transaction {
        Telefono.update({ Telefono.id eq id }) {
            it[nombre] = telefonoModel.nombre
            it[telefono] = telefonoModel.telefono
        }
    }
}
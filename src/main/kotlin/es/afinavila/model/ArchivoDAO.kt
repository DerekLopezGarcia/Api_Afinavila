package es.afinavila.model

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.io.File

object Archivo : IntIdTable() {
    val nombre = varchar("nombre", 100)
    val descripcion = varchar("descripcion", 200)
    val comunidadId = integer("comunidadId").references(Comunidad.id)
}

data class ArchivoModel(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val comunidadId: Int
)

fun ResultRow.toArchivo() = ArchivoModel(
    id = this[Archivo.id].value,
    nombre = this[Archivo.nombre],
    descripcion = this[Archivo.descripcion],
    comunidadId = this[Archivo.comunidadId]
)

object ArchivoDAO {
    fun addArchivo(archivoModel: ArchivoModel, file: File) {
        val directory = File(file.parent)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        file.copyTo(File(directory, file.name), overwrite = true)
        transaction {
            Archivo.insert {
                it[nombre] = archivoModel.nombre
                it[descripcion] = archivoModel.descripcion
                it[comunidadId] = archivoModel.comunidadId
            }
        }
    }

    fun deleteArchivo(id: Int): Boolean = transaction {
        Archivo.deleteWhere { Archivo.id eq id } > 0
    }

    fun getArchivo(id: Int): ArchivoModel? = transaction {
        Archivo.select { Archivo.id eq id }
            .map { it.toArchivo() }
            .singleOrNull()
    }

    fun getArchivosByComunidad(comunidadId: Int): List<ArchivoModel> = transaction {
        Archivo.select { Archivo.comunidadId eq comunidadId }
            .map { it.toArchivo() }
    }
}
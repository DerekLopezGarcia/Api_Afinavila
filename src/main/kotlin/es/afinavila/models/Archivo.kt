package es.afinavila.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

object ArchivoTable : IntIdTable("archivo") {
    val nombre = varchar("nombre", 200)
    val nombreMostrar = varchar("nombre_mostrar", 200)
    val descripcion = varchar("descripcion", 500)
    val comunidadId = integer("comunidad_id").references(ComunidadTable.id)
}

data class ArchivoResponse(
    val id: Int,
    val nombre: String,
    val nombreMostrar: String,
    val descripcion: String,
    val comunidadId: Int,
    val categoria: String
)

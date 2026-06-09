package es.afinavila.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

object ComunidadTable : IntIdTable("comunidad") {
    val nombre = varchar("nombre", 100)
    val numeroComunidad = varchar("numero_comunidad", 2).default("")
    val claveAcceso = varchar("clave_acceso", 20).default("")
    val codigoAcceso = varchar("codigo_acceso", 20)
}

data class ComunidadResponse(
    val id: Int,
    val nombre: String,
    val numeroComunidad: String,
    val claveAcceso: String,
    val codigoAcceso: String
)

data class ComunidadRequest(
    val nombre: String,
    val numeroComunidad: String,
    val claveAcceso: String
) {
    val codigoAcceso: String get() = "$numeroComunidad $claveAcceso"
}

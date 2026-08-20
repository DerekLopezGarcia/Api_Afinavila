package es.afinavila.models

import org.jetbrains.exposed.sql.Table

object SessionTable : Table("session") {
    val tokenHash = varchar("token_hash", 64)
    val kind = varchar("kind", 10)
    val codigoAcceso = varchar("codigo_acceso", 20).nullable()
    val comunidadId = integer("comunidad_id").nullable()
    val comunidadNombre = varchar("comunidad_nombre", 100).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(tokenHash)
}

package es.afinavila.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import es.afinavila.models.ComunidadTable
import es.afinavila.models.ArchivoTable

fun Application.configureDatabases(dbPath: String) {
    Database.connect("jdbc:sqlite:$dbPath?foreign_keys=ON&journal_mode=WAL", "org.sqlite.JDBC")
    transaction {
        SchemaUtils.createMissingTablesAndColumns(ComunidadTable, ArchivoTable)
    }
}

package es.afinavila

import es.afinavila.plugins.*
import es.afinavila.services.ArchivoService
import es.afinavila.services.AuthService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8081
    val dbPath = System.getenv("DB_PATH") ?: "data/afinavila.db"
    val filesPath = System.getenv("FILES_PATH") ?: "comunidades"
    val jwtSecret = System.getenv("JWT_SECRET") ?: "change-me"
    val adminPassword = System.getenv("ADMIN_PASSWORD") ?: "admin"

    ArchivoService.filesPath = filesPath
    AuthService.init(jwtSecret, adminPassword)

    embeddedServer(Netty, port = port) {
        module(dbPath)
    }.start(wait = true)
}

fun Application.module(dbPath: String = "data/afinavila.db") {
    configureSerialization()
    configureDatabases(dbPath)
    configureSecurity()
    configureStatusPages()
    configureRouting()

    ArchivoService.syncFromDisk()
}

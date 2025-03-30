// src/main/kotlin/es/afinavila/Application.kt

package es.afinavila

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import es.afinavila.controler.ComunidadControler
import es.afinavila.controler.ArchivoControler
import es.afinavila.model.ComunidadModel
import java.io.File

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureRouting()
    configureDatabases()
    initializeDatabase()
}

fun Application.configureDatabases() {
    val dbUrl = environment.config.property("ktor.database.url").getString()
    val dbDriver = environment.config.property("ktor.database.driver").getString()
    val dbUser = environment.config.property("ktor.database.user").getString()
    val dbPassword = environment.config.property("ktor.database.password").getString()

    Database.connect(
        url = dbUrl,
        driver = dbDriver,
        user = dbUser,
        password = dbPassword
    )
}

fun Application.initializeDatabase() {
    val comunidadControler = ComunidadControler()
    val archivoControler = ArchivoControler()
    val comunidadesDir = File("comunidades")

    if (comunidadesDir.exists() && comunidadesDir.isDirectory) {
        comunidadesDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val codigoAcceso = dir.name
                val comunidad = comunidadControler.getComunidadByCodigoAcceso(codigoAcceso)
                if (comunidad == null) {
                    val newComunidad = ComunidadModel(nombre = codigoAcceso, codigoAcceso = codigoAcceso)
                    comunidadControler.addComunidad(newComunidad)
                }
                val comunidadId = comunidadControler.getComunidadByCodigoAcceso(codigoAcceso)?.id
                comunidadId?.let {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            archivoControler.addArchivo(it, file)
                        }
                    }
                }
            }
        }
    }
}
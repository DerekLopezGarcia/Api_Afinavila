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

fun initializeDatabase() {
    val comunidadControler = ComunidadControler()
    val archivoControler = ArchivoControler()
    val comunidadesDir = File("comunidades")

    // Lista de códigos de acceso y sus nombres correspondientes
    val codigoAccesoToNombre = mapOf(
        "af3w7h" to "C.P. A. RODRÍGUEZ SAHAGÚN 36",
        "gt59kj" to "C.P. CAMINO DEL GANSINO 44-4",
        "mg2avi" to "C.P. AVDA. DCHOS HUMANOS 40-42",
        "al6po7" to "C.P. ALFONSO VI Nº 3",
        "mq9avi" to "C.P. MARQUES DE S DOMINGO 9 P2",
        "cgv1mq6" to "C.P. MARQUES DE S DOMINGO 2",
        "lv16sep" to "C.P. LUIS VALERO 16",
        "bancas4" to "C.P. BANDERAS DE CASTILLA Nº 8",
        "fr16av" to "C.P. AVDA. JOSÉ ANT 3– EL FRESNO",
        "cmv22av" to "C.P. CAPITÁN MÉNDEZ VIGO 22",
        "vico7av" to "C.P. VIRGEN DE COVADONGA 7",
        "cpd1av" to "C.P. DALIA 1",
        "vall44av" to "C.P. VALLESPÍN 44",
        "cpaq2av" to "C.P. ALFONSO QUEREJAZU Nº 2",
        "dr50pz6" to "C.P. SAN BENITO & JESÚS GALÁN 50",
        "pebe1blo5" to "C.P. PRÍNCIPE DON JUAN 1",
        "clo8avi" to "C.P. LOGROÑO 8",
        "pzlo4avi" to "C.P. LAS LOSILLAS 4",
        "cbil71avi" to "C.P. BILBAO 71",
        "avju24av" to "C.P. AVDA. JUVENTUD 24",
        "ars16avi" to "C.P. A RODRÍGUEZ SAHAGÚN 16",
        "urr4avi" to "C.P. DOÑA URRACA Nº 4",
        "cpmsavi" to "C.P. MADRESELVA",
        "so1014avi" to "C.P. SORIA 10-12-14-16",
        "so915avi" to "C.P. SORIA 9-11-13-15",
        "cmv39avi" to "C.P. CAPITÁN MÉNDEZ VIGO 39",
        "ser2avi" to "C.P. SERROTA 2",
        "ruma2avi" to "C.P. RUFINO MARTIN 2",
        "er7avi" to "C.P. LAS ERAS 7",
        "so11avi" to "C.P. SORIA 11",
        "so13avi" to "C.P. SORIA 13",
        "lamo4avi" to "C.P. LA MORAÑA 4",
        "reab23elba" to "C.P. REAL DE ABAJO 22 – EL BARRACO",
        "avpo22av" to "C.P. AVDA. DE PORTUGAL 22",
        "traig2elba" to "C.P. TRAV. IGLESIA 2 – EL BARRACO",
        "rum19avi" to "C.P. RUFINO MARTIN 19",
        "caychoavi" to "C.P. CANALES Y CHOZAS 9",
        "so15avi" to "C.P. SORIA 15",
        "badon10avi" to "C.P. BAJADA DON ALONSO 10",
        "rrcc7cc10" to "C.P. R.R.C.C. 7 y C.C. 8",
        "cmv1517avi" to "C.P. CAPITÁN MÉNDEZ VIGO 15-17",
        "rrcc9avi" to "C.P. R.R.C.C 9",
        "avhum60" to "C.P. AVDA. DERECHOS HUMANOS 58",
        "marav2avi" to "C.P. MAURICE RAVELL 2",
        "rerichiavi" to "C.P. RESIDENCIAL RIO CHICO",
        "cc10av" to "C.P. COMUNEROS DE CASTILLA 10",
        "tor18av" to "C.P. GARAJES TORDESILLAS 18",
        "cerovi3av" to "C.P. CERCO DE OVIEDO Nº 3",
        "badon28" to "C.P. BAJADA DON ALONSO 28",
        "cmota3av" to "C.P. CASTILLO DE LA MOTA 3",
        "ruma26av" to "C.P. RUFINO MARTIN Nº 26",
        "stodom19av" to "C.P. SANTO DOMINGO 17-19",
        "dahe18" to "C.P. DAVID HERRERO 18",
        "veresq33" to "C.P. VEREDA DEL ESQUILEO 33 35 37",
        "vichi2" to "C.P. VIRGEN DE CHILLA Nº 2",
        "frfravi" to "C.P. FRANCISCO FRANCO"
    )

    if (comunidadesDir.exists() && comunidadesDir.isDirectory) {
        comunidadesDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val codigoAcceso = dir.name
                val comunidad = comunidadControler.getComunidadByCodigoAcceso(codigoAcceso)
                if (comunidad == null) {
                    val nombre = codigoAccesoToNombre[codigoAcceso] ?: codigoAcceso
                    val newComunidad = ComunidadModel(nombre = nombre, codigoAcceso = codigoAcceso)
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
package es.afinavila.controler

import es.afinavila.model.ArchivoDAO
import es.afinavila.model.ArchivoModel
import java.io.File

class ArchivoControler {

    fun getArchivo(id: Int) = ArchivoDAO.getArchivo(id)
    fun addArchivo(id: Int, file: File) {
        val comunidad = ComunidadControler().getComunidad(id)
        val directory = File(comunidad!!.codigoAcceso)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val destinationFile = File(directory, file.name)
        if (destinationFile.exists()) {
            destinationFile.delete() // Overwrite the existing file
        }
        file.copyTo(destinationFile)
        val archivoModel = ArchivoModel(
            id = 0, nombre = file.name, descripcion = setDescription(file), comunidadId = comunidad.id
        )
        ArchivoDAO.addArchivo(archivoModel)
    }

    fun deleteArchivo(id: Int) {
        val archivo = ArchivoDAO.getArchivo(id)
        val file = File(archivo!!.nombre)
        file.delete()
        ArchivoDAO.deleteArchivo(id)
    }

    private fun setDescription(file: File): String {
        val fileName = file.nameWithoutExtension
        val fileType = if (fileName.startsWith("acta")) "acta" else fileName.substring(0, 3)
        val fileYear =
            if (fileType == "acta") fileName.substring(4, 10) else fileName.substring(3, 7)

        val typeDescription = when (fileType) {
            "acta" -> "Acta"
            "evo" -> "Evolución"
            "ene" -> "Extracto Enero"
            "feb" -> "Extracto Febrero"
            "mar" -> "Extracto Marzo"
            "abr" -> "Extracto Abril"
            "may" -> "Extracto Mayo"
            "jun" -> "Extracto Junio"
            "jul" -> "Extracto Julio"
            "ago" -> "Extracto Agosto"
            "sep" -> "Extracto Septiembre"
            "oct" -> "Extracto Octubre"
            "nov" -> "Extracto Noviembre"
            "dic" -> "Extracto Diciembre"
            else -> "Desconocido"
        }

        return if (fileType == "acta") {
            "$typeDescription del día ${fileYear.substring(0, 2)} de ${
                fileYear.substring(
                    2, 4
                )
            } del año ${fileYear.substring(4, 6)}"
        } else {
            "$typeDescription del año $fileYear"
        }
    }
    fun updateArchivos(id: Int) {
        val comunidad = ComunidadControler().getComunidad(id)
        val directory = File(comunidad!!.codigoAcceso)
        if (directory.exists() && directory.isDirectory) {
            val existingFiles = ArchivoDAO.getArchivosByComunidad(id).map { it.nombre }.toSet()
            directory.listFiles()?.forEach { file ->
                if (file.isFile && file.name !in existingFiles) {
                    val archivoModel = ArchivoModel(
                        id = 0, nombre = file.name, descripcion = setDescription(file), comunidadId = comunidad.id
                    )
                    ArchivoDAO.addArchivo(archivoModel)
                }
            }
        }
    }
}
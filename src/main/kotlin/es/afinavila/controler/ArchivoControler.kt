package es.afinavila.controler

import es.afinavila.model.ArchivoDAO
import es.afinavila.model.ArchivoModel
import java.io.File

class ArchivoControler {

    fun getArchivo(id: Int): String? {
        val archivo = ArchivoDAO.getArchivo(id)
        val comunidad = archivo?.comunidadId?.let { ComunidadControler().getComunidad(it) }
        return if (archivo != null && comunidad != null) {
            "https://www.afinavila.es/comunidades/${comunidad.codigoAcceso}/${archivo.nombre}"
        } else {
            null
        }
    }

    fun addArchivo(id: Int, file: File) {
        val comunidad = ComunidadControler().getComunidad(id)
        val archivoModel = comunidad?.id?.let {
            ArchivoModel(
                id = 0, nombre = file.name, descripcion = setDescription(file), comunidadId = it
            )
        }
        archivoModel?.let { ArchivoDAO.addArchivo(it,file) }
    }

    fun deleteArchivo(id: Int) {
        val archivo = ArchivoDAO.getArchivo(id)
        val file = File(archivo!!.nombre)
        file.delete()
        ArchivoDAO.deleteArchivo(id)
    }

    private fun setDescription(file: File): String {
        val fileName = file.nameWithoutExtension
        if (fileName.length < 4) {
            return fileName
        }
        val fileType = if (fileName.startsWith("acta")) "acta" else fileName.substring(0, 3)
        val fileYear = if (fileType == "acta") {
            if (fileName.length < 10) {
                return fileName
            }
            "${fileName.substring(4, 6)}-${fileName.substring(6, 8)}-${fileName.substring(8, 10)}"
        } else {
            if (fileName.length < 7) {
                return fileName
            }
            "${fileName.substring(3, 5)}-${fileName.substring(5, 7)}"
        }

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
            else -> fileName
        }

        return if (fileType == "acta") {
            "$typeDescription del día ${fileYear.substring(0, 2)} del mes ${fileYear.substring(3, 5)} del año ${fileYear.substring(6, 8)}"
        } else if (typeDescription == fileName) {
            typeDescription
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
                    comunidad.id?.let { comunidadId ->
                        val archivoModel = ArchivoModel(
                            id = 0, nombre = file.name, descripcion = setDescription(file), comunidadId = comunidadId
                        )
                        ArchivoDAO.addArchivo(archivoModel, file)
                    }
                }
            }
        }
    }
    fun getArchivosByComunidad(comunidadId: Int) = ArchivoDAO.getArchivosByComunidad(comunidadId)

}
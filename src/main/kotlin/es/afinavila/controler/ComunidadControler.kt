package es.afinavila.controler

import es.afinavila.model.ComunidadDAO
import es.afinavila.model.ComunidadModel
import java.io.File

class ComunidadControler {
    //CRUD Comunidades
    fun getComunidades() = ComunidadDAO.getComunidades()
    fun addComunidad(comunidad: ComunidadModel) {
        ComunidadDAO.addComunidad(comunidad)
        val directory = File("comunidades/${comunidad.codigoAcceso}")
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }
    fun deleteComunidad(id: Int) = ComunidadDAO.deleteComunidad(id)
    fun updateComunidad(id: Int, comunidad: ComunidadModel) = ComunidadDAO.updateComunidad(id, comunidad)>0
    fun getComunidad(id: Int) = ComunidadDAO.getComunidad(id)
    fun getComunidadByCodigoAcceso(codigoAcceso: String) = ComunidadDAO.getComunidadByCodigoAcceso(codigoAcceso)

}
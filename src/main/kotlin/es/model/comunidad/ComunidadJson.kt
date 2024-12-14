package com.afinavila.model.cominidad

import es.model.comunidad.Comunidad

object ComunidadJson {
    private val mockjson = """
        [
        {
            "id": 1,
            "nombre": "Comunidad 1",
            "codigoAceso": "123456"
        }
        ]
    """
    private val mockComunidad = mutableListOf(
        Comunidad(1, "Comunidad 1", "123456")
    )

    fun getComunidad(): List<Comunidad> = mockComunidad
    fun getComunidadJson(): String = mockjson
    fun addComunidad(comunidad: Comunidad) {
        mockComunidad.add(comunidad)
    }
    fun deleteComunidad(id: Int):Boolean = mockComunidad.removeIf { it.id == id }
}
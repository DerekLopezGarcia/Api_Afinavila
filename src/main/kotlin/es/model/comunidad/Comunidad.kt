package es.model.comunidad

import kotlinx.serialization.Serializable

@Serializable
data class Comunidad(
    val id: Int,
    val nombre: String,
    val codigoAcceso: String,
)

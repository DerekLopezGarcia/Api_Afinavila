package es.afinavila.services

data class ParsedFileName(
    val nombreMostrar: String,
    val descripcion: String,
    val categoria: String
)

object FileNameParser {
    private val meses = mapOf(
        "ene" to "Enero", "feb" to "Febrero", "mar" to "Marzo",
        "abr" to "Abril", "may" to "Mayo", "jun" to "Junio",
        "jul" to "Julio", "ago" to "Agosto", "sep" to "Septiembre",
        "oct" to "Octubre", "nov" to "Noviembre", "dic" to "Diciembre"
    )

    fun parse(filename: String): ParsedFileName {
        val name = filename.substringBeforeLast(".")
        val lower = name.lowercase()

        return when {
            lower.startsWith("acta") -> parseActa(name)
            lower.startsWith("evo") -> parseEvo(name)
            meses.keys.any { lower.startsWith(it) } -> parseExtracto(name)
            lower.startsWith("cuota") -> parseCuota(name)
            else -> ParsedFileName(
                nombreMostrar = name,
                descripcion = name,
                categoria = "Otros"
            )
        }
    }

    private fun parseActa(name: String): ParsedFileName {
        if (name.length < 10) return ParsedFileName(name, name, "Actas")
        val dd = name.substring(4, 6)
        val mm = name.substring(6, 8).toIntOrNull() ?: return ParsedFileName(name, name, "Actas")
        val yy = name.substring(8, 10)
        val mesNombre = meses.values.elementAtOrNull(mm - 1) ?: return ParsedFileName(name, name, "Actas")
        return ParsedFileName(
            nombreMostrar = "Acta $dd/$mm/20$yy",
            descripcion = "Acta de reunión del $dd de $mesNombre de 20$yy",
            categoria = "Actas"
        )
    }

    private fun parseEvo(name: String): ParsedFileName {
        if (name.length < 7) return ParsedFileName(name, name, "Evoluciones")
        val yy = name.substring(3, 5)
        val mm = name.substring(5, 7).toIntOrNull() ?: return ParsedFileName(name, name, "Evoluciones")
        val semestre = if (mm <= 6) "1er sem." else "2do sem."
        val periodo = if (mm <= 6) "Enero a Junio" else "Julio a Diciembre"
        return ParsedFileName(
            nombreMostrar = "Evolución $semestre 20$yy",
            descripcion = "Evolución anual — $periodo 20$yy",
            categoria = "Evoluciones"
        )
    }

    private fun parseExtracto(name: String): ParsedFileName {
        if (name.length < 4) return ParsedFileName(name, name, "Extractos")
        val prefijo = name.substring(0, 3).lowercase()
        val yy = if (name.length >= 5) name.substring(3, 5) else ""
        val mes = meses[prefijo] ?: return ParsedFileName(name, name, "Extractos")
        return ParsedFileName(
            nombreMostrar = "$mes 20$yy",
            descripcion = "Extracto mensual — $mes 20$yy",
            categoria = "Extractos"
        )
    }

    private fun parseCuota(name: String): ParsedFileName {
        val yearSuffix = name.substring(5)
        if (yearSuffix.isEmpty()) return ParsedFileName(name, name, "Otros")
        val year = if (yearSuffix.length == 2) "20$yearSuffix" else yearSuffix
        return ParsedFileName(
            nombreMostrar = "Cuota $year",
            descripcion = "Cuota anual $year",
            categoria = "Otros"
        )
    }
}

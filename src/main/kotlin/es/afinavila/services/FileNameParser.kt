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
        val suffix = name.substring(3)

        val fullYear = suffix.toIntOrNull()
        if (fullYear != null && fullYear >= 2000 && fullYear <= 2100) {
            return ParsedFileName(
                nombreMostrar = "Evolución $fullYear",
                descripcion = "Evolución anual — $fullYear",
                categoria = "Evoluciones"
            )
        }

        val yy = name.substring(3, 5)
        val mm = name.substring(5, 7).toIntOrNull()
        if (mm != null && mm in 1..12) {
            val semestre = if (mm <= 6) "1er sem." else "2do sem."
            val periodo = if (mm <= 6) "Enero a Junio" else "Julio a Diciembre"
            return ParsedFileName(
                nombreMostrar = "Evolución $semestre 20$yy",
                descripcion = "Evolución anual — $periodo 20$yy",
                categoria = "Evoluciones"
            )
        }

        val yy2Raw = name.substring(5, 7)
        val yy1 = yy.toIntOrNull()
        val yy2 = yy2Raw.toIntOrNull()
        if (yy1 != null && yy2 != null && yy1 in 0..99 && yy2 in 0..99) {
            return ParsedFileName(
                nombreMostrar = "Evolución 20$yy – 20$yy2Raw",
                descripcion = "Evolución — de 20$yy a 20$yy2Raw",
                categoria = "Evoluciones"
            )
        }

        return ParsedFileName(name, name, "Evoluciones")
    }

    private fun parseExtracto(name: String): ParsedFileName {
        if (name.length < 4) return ParsedFileName(name, name, "Extractos")
        val prefijo = name.substring(0, 3).lowercase()
        val mes = meses[prefijo] ?: return ParsedFileName(name, name, "Extractos")
        val yearSuffix = name.substring(3)
        val year = when {
            yearSuffix.length == 4 && yearSuffix.toIntOrNull() != null -> yearSuffix
            yearSuffix.length >= 2 -> "20${yearSuffix.substring(0, 2)}"
            else -> ""
        }
        if (year.isEmpty()) return ParsedFileName(name, name, "Extractos")
        return ParsedFileName(
            nombreMostrar = "$mes $year",
            descripcion = "Extracto mensual — $mes $year",
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

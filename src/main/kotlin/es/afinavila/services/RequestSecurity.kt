package es.afinavila.services

import io.ktor.server.application.ApplicationCall

object RequestSecurity {
    fun sameOrigin(call: ApplicationCall): Boolean {
        val origin = call.request.headers["Origin"] ?: call.request.headers["Referer"] ?: return true
        val allowed = (System.getenv("ALLOWED_ORIGINS")
            ?: "https://www.afinavila.es,https://afinavila.es")
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
        return allowed.any { origin == it || origin.startsWith("$it/") }
    }
}

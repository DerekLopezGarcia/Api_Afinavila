package es.afinavila.services

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object LoginRateLimiter {
    private val attempts = ConcurrentHashMap<String, MutableList<Long>>()
    private const val MAX_ATTEMPTS = 5
    private const val WINDOW_MS = 60_000L

    fun tryAcquire(ip: String): Boolean {
        val now = System.currentTimeMillis()
        val list = attempts.getOrPut(ip) { mutableListOf() }
        list.removeAll { now - it > WINDOW_MS }
        if (list.size >= MAX_ATTEMPTS) return false
        list.add(now)
        return true
    }
}

object SessionManager {
    private val sessions = ConcurrentHashMap<String, SessionData>()
    private val adminSessions = ConcurrentHashMap<String, AdminSessionData>()
    private const val SESSION_TTL = 3600_000L

    data class SessionData(val codigoAcceso: String, val comunidadId: Int, val comunidadNombre: String, val createdAt: Long)
    data class AdminSessionData(val createdAt: Long)

    // === Sesiones de comunidad ===
    fun create(codigoAcceso: String, comunidadId: Int, comunidadNombre: String): String {
        val token = UUID.randomUUID().toString().replace("-", "")
        sessions[token] = SessionData(codigoAcceso, comunidadId, comunidadNombre, System.currentTimeMillis())
        return token
    }

    fun validate(token: String): SessionData? {
        val data = sessions[token] ?: return null
        if (System.currentTimeMillis() - data.createdAt > SESSION_TTL) {
            sessions.remove(token)
            return null
        }
        return data
    }

    fun remove(token: String) {
        sessions.remove(token)
    }

    // === Sesiones de admin ===
    fun createAdminSession(): String {
        val token = UUID.randomUUID().toString().replace("-", "")
        adminSessions[token] = AdminSessionData(System.currentTimeMillis())
        return token
    }

    fun validateAdmin(token: String): Boolean {
        val data = adminSessions[token] ?: return false
        if (System.currentTimeMillis() - data.createdAt > SESSION_TTL) {
            adminSessions.remove(token)
            return false
        }
        return true
    }

    fun removeAdmin(token: String) {
        adminSessions.remove(token)
    }
}

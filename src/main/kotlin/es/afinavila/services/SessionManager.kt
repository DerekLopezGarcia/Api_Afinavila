package es.afinavila.services

import es.afinavila.models.SessionTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object LoginRateLimiter {
    private val attempts = ConcurrentHashMap<String, MutableList<Long>>()
    private const val MAX_ATTEMPTS = 5
    private const val WINDOW_MS = 60_000L
    private const val MAX_TRACKED_KEYS = 10_000

    fun tryAcquire(ip: String): Boolean {
        if (attempts.size > MAX_TRACKED_KEYS) attempts.clear()
        val now = System.currentTimeMillis()
        val list = attempts.getOrPut(ip) { mutableListOf() }
        return synchronized(list) {
            list.removeAll { now - it > WINDOW_MS }
            if (list.size >= MAX_ATTEMPTS) return@synchronized false
            list.add(now)
            true
        }
    }
}

object SessionManager {
    private const val SESSION_TTL = 3600_000L
    private const val CLIENT_KIND = "client"
    private const val ADMIN_KIND = "admin"

    data class SessionData(val codigoAcceso: String, val comunidadId: Int, val comunidadNombre: String, val createdAt: Long)

    private fun newToken(): String = UUID.randomUUID().toString().replace("-", "")

    private fun hash(token: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
    )

    fun create(codigoAcceso: String, comunidadId: Int, comunidadNombre: String): String {
        val token = newToken()
        val now = System.currentTimeMillis()
        transaction {
            SessionTable.insert {
                it[tokenHash] = hash(token)
                it[kind] = CLIENT_KIND
                it[SessionTable.codigoAcceso] = codigoAcceso
                it[SessionTable.comunidadId] = comunidadId
                it[SessionTable.comunidadNombre] = comunidadNombre
                it[createdAt] = now
            }
        }
        return token
    }

    fun validate(token: String): SessionData? = transaction {
        val row = SessionTable.select {
            (SessionTable.tokenHash eq hash(token)) and (SessionTable.kind eq CLIENT_KIND)
        }.singleOrNull() ?: return@transaction null
        if (System.currentTimeMillis() - row[SessionTable.createdAt] > SESSION_TTL) {
            SessionTable.deleteWhere { SessionTable.tokenHash eq hash(token) }
            return@transaction null
        }
        SessionData(
            row[SessionTable.codigoAcceso] ?: return@transaction null,
            row[SessionTable.comunidadId] ?: return@transaction null,
            row[SessionTable.comunidadNombre] ?: return@transaction null,
            row[SessionTable.createdAt]
        )
    }

    fun remove(token: String) {
        transaction { SessionTable.deleteWhere { SessionTable.tokenHash eq hash(token) } }
    }

    fun createAdminSession(): String {
        val token = newToken()
        transaction {
            SessionTable.insert {
                it[tokenHash] = hash(token)
                it[kind] = ADMIN_KIND
                it[createdAt] = System.currentTimeMillis()
            }
        }
        return token
    }

    fun validateAdmin(token: String): Boolean = transaction {
        val row = SessionTable.select {
            (SessionTable.tokenHash eq hash(token)) and (SessionTable.kind eq ADMIN_KIND)
        }.singleOrNull() ?: return@transaction false
        if (System.currentTimeMillis() - row[SessionTable.createdAt] > SESSION_TTL) {
            SessionTable.deleteWhere { SessionTable.tokenHash eq hash(token) }
            false
        } else true
    }

    fun removeAdmin(token: String) = remove(token)
}

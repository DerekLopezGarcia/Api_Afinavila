package es.afinavila.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object AuthService {
    private var secret: String = "change-me"
    private var adminPassword: String = "admin"

    fun init(envSecret: String, envPassword: String) {
        secret = envSecret.ifBlank { "change-me" }
        adminPassword = envPassword.ifBlank { "admin" }
    }

    fun validatePassword(password: String): Boolean = password == adminPassword

    fun generateToken(): String {
        val algorithm = Algorithm.HMAC256(secret)
        return JWT.create()
            .withSubject("admin")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 86400000))
            .sign(algorithm)
    }

    fun getVerifier(): Algorithm = Algorithm.HMAC256(secret)
}

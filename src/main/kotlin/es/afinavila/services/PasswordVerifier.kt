package es.afinavila.services

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordVerifier {
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256

    fun matches(candidate: String): Boolean {
        val encoded = System.getenv("ADMIN_PASSWORD_HASH")
        if (!encoded.isNullOrBlank()) return verifyPbkdf2(candidate, encoded)
        val legacy = System.getenv("ADMIN_PASSWORD") ?: return false
        return MessageDigest.isEqual(candidate.toByteArray(), legacy.toByteArray())
    }

    fun hash(password: CharArray, random: SecureRandom = SecureRandom()): String {
        val salt = ByteArray(16).also(random::nextBytes)
        val derived = derive(password, salt, ITERATIONS)
        return listOf(
            "pbkdf2-sha256",
            ITERATIONS,
            Base64.getUrlEncoder().withoutPadding().encodeToString(salt),
            Base64.getUrlEncoder().withoutPadding().encodeToString(derived)
        ).joinToString("|")
    }

    private fun verifyPbkdf2(password: String, encoded: String): Boolean = runCatching {
        val parts = encoded.split('|')
        require(parts.size == 4 && parts[0] == "pbkdf2-sha256")
        val iterations = parts[1].toInt()
        val salt = Base64.getUrlDecoder().decode(parts[2])
        val expected = Base64.getUrlDecoder().decode(parts[3])
        MessageDigest.isEqual(expected, derive(password.toCharArray(), salt, iterations))
    }.getOrDefault(false)

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

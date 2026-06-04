package es.afinavila.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import es.afinavila.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {
    val verifier = AuthService.getVerifier()

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JWT.require(verifier).build())
            validate { credential ->
                if (credential.payload.subject == "admin") JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    status = HttpStatusCode.Unauthorized,
                    message = mapOf("error" to "Token invalido o expirado")
                )
            }
        }
    }
}

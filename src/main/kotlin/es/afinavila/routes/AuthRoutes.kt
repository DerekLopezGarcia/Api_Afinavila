package es.afinavila.routes

import es.afinavila.services.AuthService
import es.afinavila.models.LoginRequest
import es.afinavila.models.LoginResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    route("/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            if (AuthService.validatePassword(request.password)) {
                val token = AuthService.generateToken()
                call.respond(LoginResponse(token))
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Contraseña incorrecta"))
            }
        }
    }
}

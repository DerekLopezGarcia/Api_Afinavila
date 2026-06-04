package es.afinavila.models

data class LoginRequest(val password: String)

data class LoginResponse(val token: String)

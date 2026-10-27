package com.udenar.flashly.data

/** Convierte excepciones técnicas en mensajes entendibles para el usuario. */
fun mensajeError(e: Throwable): String {
    val m = e.message.orEmpty()
    val bajo = m.lowercase()
    return when {
        "invalid login credentials" in bajo -> "Correo o contraseña incorrectos"
        "already registered" in bajo || "already been registered" in bajo -> "Ese correo ya está registrado"
        "rate limit" in bajo -> "Demasiados intentos, espera un momento"
        "unable to resolve host" in bajo || "timeout" in bajo || "UnknownHost" in e.toString() ->
            "No hay conexión con el servidor. Revisa tu Internet"
        m.isBlank() -> "Ocurrió un error inesperado"
        else -> m.lineSequence().first().take(200)
    }
}
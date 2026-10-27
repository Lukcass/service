package com.udenar.flashly.data

/** Pregunta tal como se edita en el formulario (4 opciones, [correcta] = índice 0..3 o -1 si falta). */
data class PreguntaForm(
    val enunciado: String,
    val opciones: List<String>,
    val correcta: Int,
)

/** Devuelve un mensaje de error si el mazo no es válido, o null si todo está bien. */
fun validarMazo(nombre: String, preguntas: List<PreguntaForm>): String? {
    if (nombre.isBlank()) return "El mazo necesita un nombre"
    if (preguntas.isEmpty()) return "Agrega al menos una pregunta"
    preguntas.forEachIndexed { i, p ->
        val n = i + 1
        if (p.enunciado.isBlank()) return "La pregunta $n no tiene enunciado"
        if (p.opciones.any { it.isBlank() }) return "Completa todas las opciones de la pregunta $n"
        val distintas = p.opciones.map { it.trim().lowercase() }.toSet().size
        if (distintas != p.opciones.size) return "Las opciones de la pregunta $n deben ser distintas"
        if (p.correcta !in p.opciones.indices) return "Marca la respuesta correcta de la pregunta $n"
    }
    return null
}
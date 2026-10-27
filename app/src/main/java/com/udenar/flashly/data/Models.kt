package com.udenar.flashly.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Mazo(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    @SerialName("creador_id") val creadorId: String,
    @SerialName("es_publico") val esPublico: Boolean,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class NuevoMazo(
    val id: String,
    val nombre: String,
    val descripcion: String?,
    @SerialName("es_publico") val esPublico: Boolean,
)

@Serializable
data class Pregunta(
    val id: String,
    @SerialName("mazo_id") val mazoId: String,
    val enunciado: String,
    val opciones: List<String>,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class NuevaPregunta(
    val id: String,
    @SerialName("mazo_id") val mazoId: String,
    val enunciado: String,
    val opciones: List<String>,
)

@Serializable
data class NuevaRespuesta(
    @SerialName("pregunta_id") val preguntaId: String,
    @SerialName("indice_correcto") val indiceCorrecto: Int,
)

@Serializable
data class NuevoResultado(
    @SerialName("mazo_id") val mazoId: String,
    val puntuacion: Int,
    @SerialName("total_preguntas") val totalPreguntas: Int,
)

@Serializable
data class MazoNombre(val nombre: String)

@Serializable
data class Resultado(
    val id: String,
    val puntuacion: Int,
    @SerialName("total_preguntas") val totalPreguntas: Int,
    @SerialName("created_at") val createdAt: String,
    val mazos: MazoNombre? = null,
)

/** Parámetros de la función SQL validar_respuesta (RPC). */
@Serializable
data class ValidarParams(
    @SerialName("p_pregunta_id") val preguntaId: String,
    @SerialName("p_indice") val indice: Int,
)
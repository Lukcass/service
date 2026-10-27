package com.udenar.flashly.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.util.UUID

/**
 * Único punto de acceso a Supabase (Auth + PostgreSQL).
 * La seguridad real la imponen las políticas RLS definidas en supabase/schema.sql.
 */
object FlashlyRepository {
    private val client get() = SupabaseProvider.client

    // ---------- Autenticación ----------

    /** Devuelve true si la sesión quedó iniciada (false si Supabase exige confirmar el correo). */
    suspend fun registrar(email: String, password: String): Boolean {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return client.auth.currentSessionOrNull() != null
    }

    suspend fun iniciarSesion(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun cerrarSesion() = client.auth.signOut()

    fun usuarioId(): String? = client.auth.currentUserOrNull()?.id

    // ---------- Mazos ----------

    suspend fun misMazos(): List<Mazo> {
        val uid = usuarioId() ?: return emptyList()
        return client.from("mazos").select {
            filter { eq("creador_id", uid) }
            order("created_at", Order.DESCENDING)
        }.decodeList<Mazo>()
    }

    suspend fun mazosPublicos(): List<Mazo> =
        client.from("mazos").select {
            filter { eq("es_publico", true) }
            order("created_at", Order.DESCENDING)
        }.decodeList<Mazo>()

    suspend fun mazo(id: String): Mazo =
        client.from("mazos").select {
            filter { eq("id", id) }
        }.decodeSingle<Mazo>()

    /** Crea el mazo, sus preguntas y las respuestas correctas. Si algo falla, deshace el mazo. */
    suspend fun crearMazo(
        nombre: String,
        descripcion: String?,
        esPublico: Boolean,
        preguntas: List<PreguntaForm>,
    ) {
        val mazoId = UUID.randomUUID().toString()
        client.from("mazos").insert(NuevoMazo(mazoId, nombre, descripcion, esPublico))
        try {
            val conId = preguntas.map { UUID.randomUUID().toString() to it }
            client.from("preguntas").insert(
                conId.map { (id, p) -> NuevaPregunta(id, mazoId, p.enunciado, p.opciones) }
            )
            client.from("respuestas_correctas").insert(
                conId.map { (id, p) -> NuevaRespuesta(id, p.correcta) }
            )
        } catch (e: Exception) {
            runCatching { eliminarMazo(mazoId) }
            throw e
        }
    }

    suspend fun eliminarMazo(id: String) {
        client.from("mazos").delete { filter { eq("id", id) } }
    }

    suspend fun cambiarVisibilidad(id: String, esPublico: Boolean) {
        client.from("mazos").update({ set("es_publico", esPublico) }) {
            filter { eq("id", id) }
        }
    }

    // ---------- Juego ----------

    suspend fun preguntasDeMazo(mazoId: String): List<Pregunta> =
        client.from("preguntas").select {
            filter { eq("mazo_id", mazoId) }
        }.decodeList<Pregunta>()

    /** La respuesta correcta nunca llega al dispositivo: la valida el backend (función SQL). */
    suspend fun validarRespuesta(preguntaId: String, indice: Int): Boolean =
        client.postgrest.rpc("validar_respuesta", ValidarParams(preguntaId, indice))
            .decodeAs<Boolean>()

    // ---------- Resultados ----------

    suspend fun guardarResultado(mazoId: String, puntuacion: Int, total: Int) {
        client.from("resultados").insert(NuevoResultado(mazoId, puntuacion, total))
    }

    suspend fun historial(): List<Resultado> =
        client.from("resultados").select(
            Columns.raw("id, puntuacion, total_preguntas, created_at, mazos(nombre)")
        ) {
            order("created_at", Order.DESCENDING)
        }.decodeList<Resultado>()
}
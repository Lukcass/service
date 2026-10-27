package com.udenar.flashly

import com.udenar.flashly.data.PreguntaForm
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.data.validarMazo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeckValidationTest {
    private val valida = PreguntaForm("¿Capital de Colombia?", listOf("Bogotá", "Lima", "Quito", "Caracas"), 0)

    @Test
    fun mazoCorrecto_noDevuelveError() {
        assertNull(validarMazo("Geografía", listOf(valida)))
    }

    @Test
    fun nombreVacio_devuelveError() {
        assertNotNull(validarMazo("   ", listOf(valida)))
    }

    @Test
    fun sinPreguntas_devuelveError() {
        assertEquals("Agrega al menos una pregunta", validarMazo("Mazo", emptyList()))
    }

    @Test
    fun sinRespuestaCorrecta_devuelveError() {
        assertNotNull(validarMazo("Mazo", listOf(valida.copy(correcta = -1))))
    }

    @Test
    fun opcionesRepetidas_devuelveError() {
        val repetidas = valida.copy(opciones = listOf("Sí", "sí", "No", "Tal vez"))
        assertNotNull(validarMazo("Mazo", listOf(repetidas)))
    }

    @Test
    fun opcionVacia_devuelveError() {
        val conVacia = valida.copy(opciones = listOf("Bogotá", "", "Quito", "Caracas"))
        assertNotNull(validarMazo("Mazo", listOf(conVacia)))
    }

    @Test
    fun errorDeCredenciales_seTraduce() {
        assertEquals(
            "Correo o contraseña incorrectos",
            mensajeError(Exception("Invalid login credentials"))
        )
    }
}
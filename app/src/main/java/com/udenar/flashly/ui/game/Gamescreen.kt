package com.udenar.flashly.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.FlashlyRepository
import com.udenar.flashly.data.Pregunta
import com.udenar.flashly.data.mensajeError
import kotlinx.coroutines.launch

class GameViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val mazoId: String = checkNotNull(savedStateHandle["mazoId"])

    var nombre by mutableStateOf("")
        private set
    var preguntas by mutableStateOf<List<Pregunta>>(emptyList())
        private set
    var indice by mutableIntStateOf(0)
        private set
    var puntuacion by mutableIntStateOf(0)
        private set
    var seleccion by mutableStateOf<Int?>(null)
        private set
    /** null = aún sin contestar; true/false = resultado devuelto por el backend. */
    var respuestaCorrecta by mutableStateOf<Boolean?>(null)
        private set
    var validando by mutableStateOf(false)
        private set
    var terminado by mutableStateOf(false)
        private set
    var cargando by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var guardado by mutableStateOf(false)
        private set
    var errorGuardado by mutableStateOf<String?>(null)
        private set

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            error = null
            indice = 0
            puntuacion = 0
            seleccion = null
            respuestaCorrecta = null
            terminado = false
            guardado = false
            errorGuardado = null
            try {
                nombre = FlashlyRepository.mazo(mazoId).nombre
                preguntas = FlashlyRepository.preguntasDeMazo(mazoId).shuffled()
            } catch (e: Exception) {
                error = mensajeError(e)
            }
            cargando = false
        }
    }

    fun responder(opcion: Int) {
        if (respuestaCorrecta != null || validando) return
        seleccion = opcion
        viewModelScope.launch {
            validando = true
            try {
                val ok = FlashlyRepository.validarRespuesta(preguntas[indice].id, opcion)
                respuestaCorrecta = ok
                if (ok) puntuacion++
            } catch (e: Exception) {
                seleccion = null
                error = mensajeError(e)
            }
            validando = false
        }
    }

    fun siguiente() {
        if (indice + 1 >= preguntas.size) {
            terminado = true
            guardarResultado()
        } else {
            indice++
            seleccion = null
            respuestaCorrecta = null
            error = null
        }
    }

    fun guardarResultado() {
        viewModelScope.launch {
            errorGuardado = null
            try {
                FlashlyRepository.guardarResultado(mazoId, puntuacion, preguntas.size)
                guardado = true
            } catch (e: Exception) {
                errorGuardado = mensajeError(e)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(onVolver: () -> Unit, vm: GameViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.nombre.ifBlank { "Jugar" }) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                vm.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                vm.preguntas.isEmpty() -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(vm.error ?: "Este mazo aún no tiene preguntas")
                    Button(onClick = { vm.cargar() }) { Text("Reintentar") }
                }

                vm.terminado -> Resultado(vm, onVolver)
                else -> PreguntaActual(vm)
            }
        }
    }
}

@Composable
private fun PreguntaActual(vm: GameViewModel) {
    val pregunta = vm.preguntas[vm.indice]
    val contestada = vm.respuestaCorrecta != null
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinearProgressIndicator(
            progress = { (vm.indice + 1f) / vm.preguntas.size },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Pregunta ${vm.indice + 1} de ${vm.preguntas.size}  ·  Puntos: ${vm.puntuacion}",
            style = MaterialTheme.typography.labelLarge
        )
        Text(pregunta.enunciado, style = MaterialTheme.typography.headlineSmall)

        pregunta.opciones.forEachIndexed { i, texto ->
            val color = when {
                vm.seleccion == i && vm.respuestaCorrecta == true -> MaterialTheme.colorScheme.primaryContainer
                vm.seleccion == i && vm.respuestaCorrecta == false -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Card(
                onClick = { vm.responder(i) },
                colors = CardDefaults.cardColors(containerColor = color),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(texto, Modifier.padding(16.dp))
            }
        }

        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (contestada) {
            Text(
                if (vm.respuestaCorrecta == true) "¡Correcto!" else "Incorrecto",
                style = MaterialTheme.typography.titleMedium,
                color = if (vm.respuestaCorrecta == true) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            Button(onClick = { vm.siguiente() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.indice + 1 >= vm.preguntas.size) "Ver resultado" else "Siguiente")
            }
        }
    }
}

@Composable
private fun Resultado(vm: GameViewModel, onVolver: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text("¡Partida terminada!", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${vm.puntuacion} / ${vm.preguntas.size}",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        when {
            vm.guardado -> Text("Tu puntuación quedó guardada en el historial")
            vm.errorGuardado != null -> {
                Text(vm.errorGuardado.orEmpty(), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { vm.guardarResultado() }) { Text("Reintentar guardado") }
            }
            else -> Text("Guardando resultado...")
        }
        Button(onClick = { vm.cargar() }, modifier = Modifier.fillMaxWidth()) { Text("Jugar de nuevo") }
        OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver a mis mazos") }
    }
}
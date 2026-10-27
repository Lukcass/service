package com.udenar.flashly.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.FlashlyRepository
import com.udenar.flashly.data.PreguntaForm
import com.udenar.flashly.data.mensajeError
import com.udenar.flashly.data.validarMazo
import kotlinx.coroutines.launch

private fun preguntaVacia() = PreguntaForm(enunciado = "", opciones = List(4) { "" }, correcta = -1)

class CreateDeckViewModel : ViewModel() {
    var nombre by mutableStateOf("")
        private set
    var descripcion by mutableStateOf("")
        private set
    var esPublico by mutableStateOf(false)
        private set
    var preguntas by mutableStateOf(listOf(preguntaVacia()))
        private set
    var guardando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun editarNombre(t: String) { nombre = t.take(60) }
    fun editarDescripcion(t: String) { descripcion = t.take(200) }
    fun cambiarPublico(v: Boolean) { esPublico = v }

    fun agregarPregunta() { preguntas = preguntas + preguntaVacia() }
    fun quitarPregunta(i: Int) { preguntas = preguntas.filterIndexed { idx, _ -> idx != i } }

    private fun actualizar(i: Int, cambio: (PreguntaForm) -> PreguntaForm) {
        preguntas = preguntas.mapIndexed { idx, p -> if (idx == i) cambio(p) else p }
    }

    fun editarEnunciado(i: Int, t: String) = actualizar(i) { it.copy(enunciado = t.take(300)) }
    fun editarOpcion(i: Int, j: Int, t: String) = actualizar(i) { p ->
        p.copy(opciones = p.opciones.mapIndexed { k, o -> if (k == j) t.take(120) else o })
    }
    fun marcarCorrecta(i: Int, j: Int) = actualizar(i) { it.copy(correcta = j) }

    fun guardar(onListo: () -> Unit) {
        val limpias = preguntas.map { p ->
            p.copy(enunciado = p.enunciado.trim(), opciones = p.opciones.map { it.trim() })
        }
        validarMazo(nombre, limpias)?.let {
            error = it
            return
        }
        viewModelScope.launch {
            guardando = true
            error = null
            try {
                FlashlyRepository.crearMazo(
                    nombre = nombre.trim(),
                    descripcion = descripcion.trim().ifBlank { null },
                    esPublico = esPublico,
                    preguntas = limpias
                )
                onListo()
            } catch (e: Exception) {
                error = mensajeError(e)
            }
            guardando = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeckScreen(onVolver: () -> Unit, vm: CreateDeckViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo mazo") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = vm.nombre,
                    onValueChange = vm::editarNombre,
                    label = { Text("Nombre del mazo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = vm.descripcion,
                    onValueChange = vm::editarDescripcion,
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = vm.esPublico, onCheckedChange = vm::cambiarPublico)
                    Text(
                        if (vm.esPublico) "  Público: cualquiera puede jugarlo" else "  Privado: solo tú",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            itemsIndexed(vm.preguntas) { i, p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Pregunta ${i + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            if (vm.preguntas.size > 1) {
                                IconButton(onClick = { vm.quitarPregunta(i) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Quitar pregunta ${i + 1}")
                                }
                            }
                        }
                        OutlinedTextField(
                            value = p.enunciado,
                            onValueChange = { vm.editarEnunciado(i, it) },
                            label = { Text("Enunciado") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Marca la opción correcta:", style = MaterialTheme.typography.labelMedium)
                        p.opciones.forEachIndexed { j, opcion ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = p.correcta == j,
                                    onClick = { vm.marcarCorrecta(i, j) }
                                )
                                OutlinedTextField(
                                    value = opcion,
                                    onValueChange = { vm.editarOpcion(i, j, it) },
                                    label = { Text("Opción ${j + 1}") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = vm::agregarPregunta, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Agregar pregunta")
                }
            }
            item {
                vm.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                Button(
                    onClick = { vm.guardar(onListo = onVolver) },
                    enabled = !vm.guardando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (vm.guardando) "Guardando..." else "Guardar mazo")
                }
            }
        }
    }
}
package com.udenar.flashly.ui.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.FlashlyRepository
import com.udenar.flashly.data.Mazo
import com.udenar.flashly.data.mensajeError
import kotlinx.coroutines.launch

class DecksViewModel : ViewModel() {
    val usuarioId: String? = FlashlyRepository.usuarioId()

    var mazos by mutableStateOf<List<Mazo>>(emptyList())
        private set
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var soloPublicos by mutableStateOf(false)
        private set

    fun cambiarFiltro(publicos: Boolean) {
        soloPublicos = publicos
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                mazos = if (soloPublicos) FlashlyRepository.mazosPublicos()
                else FlashlyRepository.misMazos()
            } catch (e: Exception) {
                error = mensajeError(e)
            }
            cargando = false
        }
    }

    fun alternarVisibilidad(m: Mazo) = accion { FlashlyRepository.cambiarVisibilidad(m.id, !m.esPublico) }

    fun eliminar(m: Mazo) = accion { FlashlyRepository.eliminarMazo(m.id) }

    private fun accion(bloque: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                bloque()
                cargar()
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            try {
                FlashlyRepository.cerrarSesion()
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(
    onCrear: () -> Unit,
    onJugar: (String) -> Unit,
    onHistorial: () -> Unit,
    vm: DecksViewModel = viewModel(),
) {
    // Recarga al abrir la pantalla y al volver desde otra (por ejemplo, tras crear un mazo)
    LifecycleResumeEffect(Unit) {
        vm.cargar()
        onPauseOrDispose { }
    }
    var porBorrar by remember { mutableStateOf<Mazo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flashly") },
                actions = {
                    IconButton(onClick = onHistorial) {
                        Icon(Icons.Default.History, contentDescription = "Historial de resultados")
                    }
                    IconButton(onClick = { vm.cerrarSesion() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrear) {
                Icon(Icons.Default.Add, contentDescription = "Crear mazo")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !vm.soloPublicos,
                    onClick = { vm.cambiarFiltro(false) },
                    label = { Text("Mis mazos") }
                )
                FilterChip(
                    selected = vm.soloPublicos,
                    onClick = { vm.cambiarFiltro(true) },
                    label = { Text("Públicos") }
                )
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    vm.cargando && vm.mazos.isEmpty() ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))

                    vm.error != null -> Column(
                        Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(vm.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { vm.cargar() }) { Text("Reintentar") }
                    }

                    vm.mazos.isEmpty() -> Text(
                        if (vm.soloPublicos) "Aún no hay mazos públicos"
                        else "Aún no tienes mazos. Crea el primero con el botón +",
                        Modifier.align(Alignment.Center).padding(24.dp)
                    )

                    else -> LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(vm.mazos, key = { it.id }) { mazo ->
                            MazoCard(
                                mazo = mazo,
                                esMio = mazo.creadorId == vm.usuarioId,
                                onJugar = { onJugar(mazo.id) },
                                onVisibilidad = { vm.alternarVisibilidad(mazo) },
                                onEliminar = { porBorrar = mazo }
                            )
                        }
                    }
                }
            }
        }
    }

    porBorrar?.let { mazo ->
        AlertDialog(
            onDismissRequest = { porBorrar = null },
            title = { Text("Eliminar mazo") },
            text = { Text("Se borrará \"${mazo.nombre}\" con sus preguntas y resultados. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminar(mazo)
                    porBorrar = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { porBorrar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MazoCard(
    mazo: Mazo,
    esMio: Boolean,
    onJugar: () -> Unit,
    onVisibilidad: () -> Unit,
    onEliminar: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(mazo.nombre, style = MaterialTheme.typography.titleMedium)
            if (!mazo.descripcion.isNullOrBlank()) {
                Text(mazo.descripcion, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                (if (mazo.esPublico) "Público" else "Privado") + if (esMio) " · Tuyo" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onJugar) { Text("Jugar") }
                if (esMio) {
                    IconButton(onClick = onVisibilidad) {
                        Icon(
                            if (mazo.esPublico) Icons.Default.Public else Icons.Default.Lock,
                            contentDescription = if (mazo.esPublico) "Hacer privado" else "Hacer público"
                        )
                    }
                    IconButton(onClick = onEliminar) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar mazo")
                    }
                }
            }
        }
    }
}
package com.udenar.flashly.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udenar.flashly.data.FlashlyRepository
import com.udenar.flashly.data.mensajeError
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    var cargando by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set
    var esError by mutableStateOf(false)
        private set

    fun limpiar() {
        mensaje = null
    }

    private fun validar(email: String, password: String): String? = when {
        !email.contains("@") || email.trim().length < 5 -> "Escribe un correo válido"
        password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        else -> null
    }

    fun entrar(email: String, password: String) = ejecutar(email, password) {
        FlashlyRepository.iniciarSesion(email.trim(), password)
    }

    fun registrar(email: String, password: String) = ejecutar(email, password) {
        val sesionIniciada = FlashlyRepository.registrar(email.trim(), password)
        if (!sesionIniciada) {
            esError = false
            mensaje = "Cuenta creada. Confirma tu correo e inicia sesión."
        }
    }

    private fun ejecutar(email: String, password: String, accion: suspend () -> Unit) {
        validar(email, password)?.let {
            mensaje = it
            esError = true
            return
        }
        viewModelScope.launch {
            cargando = true
            mensaje = null
            try {
                accion()
            } catch (e: Exception) {
                esError = true
                mensaje = mensajeError(e)
            }
            cargando = false
        }
    }
}

@Composable
fun AuthScreen(vm: AuthViewModel = viewModel()) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var modoRegistro by rememberSaveable { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(48.dp))
            Text("Flashly", style = MaterialTheme.typography.displaySmall)
            Text(
                if (modoRegistro) "Crea tu cuenta" else "Inicia sesión para continuar",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            vm.mensaje?.let {
                Text(
                    it,
                    color = if (vm.esError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
            Button(
                onClick = {
                    if (modoRegistro) vm.registrar(email, password) else vm.entrar(email, password)
                },
                enabled = !vm.cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        vm.cargando -> "Un momento..."
                        modoRegistro -> "Registrarme"
                        else -> "Iniciar sesión"
                    }
                )
            }
            TextButton(onClick = {
                modoRegistro = !modoRegistro
                vm.limpiar()
            }) {
                Text(if (modoRegistro) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate")
            }
        }
    }
}
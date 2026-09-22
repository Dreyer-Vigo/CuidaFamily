package com.example.cuidafamily.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.User
import com.example.cuidafamily.repository.AuthRepository
import com.example.cuidafamily.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Representa el estado de una operación de autenticación o creación de grupo.
 */
sealed class AuthStatus {
    object Idle : AuthStatus()
    object Loading : AuthStatus()
    data class Success(val user: User, val message: String) : AuthStatus()
    data class Error(val message: String) : AuthStatus()
}

/**
 * Estado general de la UI para el flujo de registro y configuración inicial.
 */
data class AuthUiState(
    val nombre: String = "",
    val email: String = "",
    val contrasenia: String = "",
    val confirmarContrasenia: String = "",
    val selectedRole: Role? = null,
    val familyGroupId: String? = null,
    val codigoInvitacion: String = "",
    val nombreGrupo: String = "",
    val subtipo: String = "", // "Hijo/a", "Enfermero/a", etc.
    val parentescoPersonalizado: String = "",
    val authStatus: AuthStatus = AuthStatus.Idle
) {
    // Lógica para habilitar el botón de registro final
    val isRegisterEnabled: Boolean
        get() = nombre.isNotBlank() &&
                email.isNotBlank() &&
                contrasenia.isNotBlank() &&
                confirmarContrasenia.isNotBlank() &&
                contrasenia == confirmarContrasenia &&
                selectedRole != null &&
                when (selectedRole) {
                    Role.ADMINISTRADOR_FAMILIAR -> nombreGrupo.isNotBlank()
                    else -> codigoInvitacion.length == 6
                }
    
    // Lógica para habilitar el botón "Continuar" en la pantalla de Subtipo
    val isSubtipoContinuarEnabled: Boolean
        get() = when (selectedRole) {
            Role.COLABORADOR -> parentescoPersonalizado.isNotBlank()
            else -> subtipo.isNotBlank()
        }
}

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onNombreChanged(nombre: String) {
        _uiState.update { it.copy(nombre = nombre) }
    }

    fun onEmailChanged(input: String) {
        _uiState.update { it.copy(email = input) }
    }

    fun onContraseniaChanged(pass: String) {
        _uiState.update { it.copy(contrasenia = pass) }
    }

    fun onConfirmarContraseniaChanged(pass: String) {
        _uiState.update { it.copy(confirmarContrasenia = pass) }
    }

    fun onRoleSelected(role: Role) {
        // Al cambiar el rol principal, reseteamos el subtipo por defecto según el rol
        val defaultSubtipo = when (role) {
            Role.ADMINISTRADOR_FAMILIAR -> "Hijo/a"
            Role.COLABORADOR -> "" // Campo libre
            Role.CUIDADOR_EXTERNO -> "Enfermero/a"
        }
        _uiState.update { it.copy(selectedRole = role, subtipo = defaultSubtipo, parentescoPersonalizado = "") }
    }

    fun onCodigoInvitacionChanged(codigo: String) {
        if (codigo.length <= 6) {
            _uiState.update { it.copy(codigoInvitacion = codigo.uppercase()) }
        }
    }

    fun onNombreGrupoChanged(nombre: String) {
        _uiState.update { it.copy(nombreGrupo = nombre) }
    }

    fun onSubtipoSelected(tipo: String) {
        _uiState.update { it.copy(subtipo = tipo) }
    }

    fun onParentescoPersonalizadoChanged(input: String) {
        _uiState.update { it.copy(parentescoPersonalizado = input) }
    }

    /**
     * Realiza el inicio de sesión y carga los datos de perfil (grupo y rol) guardados.
     */
    fun login() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.contrasenia.isBlank()) {
            _uiState.update { it.copy(authStatus = AuthStatus.Error("Completa todos los campos")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(authStatus = AuthStatus.Loading) }
            val result = AuthRepository.login(currentState.email, currentState.contrasenia)
            when (result) {
                is Result.Success -> {
                    val user = result.data
                    _uiState.update { it.copy(
                        nombre = user.nombre,
                        selectedRole = user.role,
                        familyGroupId = user.familyGroupId.ifBlank { null },
                        authStatus = AuthStatus.Success(user, "Bienvenido")
                    ) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(authStatus = AuthStatus.Error(result.exception.message ?: "Error de acceso")) }
                }
            }
        }
    }

    /**
     * Procesa el registro completo.
     */
    fun registrarUsuarioYConfigurarGrupo() {
        val currentState = _uiState.value
        if (!currentState.isRegisterEnabled) {
            if (currentState.contrasenia != currentState.confirmarContrasenia) {
                _uiState.update { it.copy(authStatus = AuthStatus.Error("Las contraseñas no coinciden")) }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(authStatus = AuthStatus.Loading) }

            val registroResult = AuthRepository.registrar(
                currentState.nombre,
                currentState.email,
                currentState.contrasenia
            )

            when (registroResult) {
                is Result.Success -> {
                    val user = registroResult.data
                    configurarGrupoParaUsuario(user, currentState)
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(authStatus = AuthStatus.Error(registroResult.exception.message ?: "Error en el registro"))
                    }
                }
            }
        }
    }

    private suspend fun configurarGrupoParaUsuario(user: User, state: AuthUiState) {
        val role = state.selectedRole ?: return
        
        // Determinamos el subtipo final a guardar
        val subtipoFinal = if (role == Role.COLABORADOR) {
            state.parentescoPersonalizado
        } else {
            state.subtipo
        }

        val result = when (role) {
            Role.ADMINISTRADOR_FAMILIAR -> {
                AuthRepository.crearFamilyGroup(state.nombreGrupo, user.id, subtipoFinal)
            }
            Role.COLABORADOR, Role.CUIDADOR_EXTERNO -> {
                AuthRepository.unirseAGrupo(state.codigoInvitacion, user.id, role, subtipoFinal)
            }
        }

        when (result) {
            is Result.Success -> {
                val familyGroup = result.data
                val msg = if (role == Role.ADMINISTRADOR_FAMILIAR) 
                    "Grupo '${familyGroup.nombre}' creado con éxito" 
                else 
                    "Te has unido al grupo correctamente"
                
                _uiState.update { it.copy(
                    familyGroupId = familyGroup.id,
                    authStatus = AuthStatus.Success(user, msg)
                ) }
            }
            is Result.Error -> {
                _uiState.update { 
                    it.copy(authStatus = AuthStatus.Error(result.exception.message ?: "Error al configurar el grupo"))
                }
            }
        }
    }

    /**
     * Revisa si existe una sesión activa y recupera el perfil completo (grupo y rol) de Firestore.
     */
    fun revisarSesion() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            viewModelScope.launch {
                try {
                    _uiState.update { it.copy(authStatus = AuthStatus.Loading) }

                    try {
                        user.getIdToken(true).await()
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Sesión inválida en servidor", e)
                        AuthRepository.cerrarSesion()
                        _uiState.value = AuthUiState()
                        return@launch
                    }
                    
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("users").document(user.uid).get().await()
                    val usuario = userDoc.toObject(User::class.java)
                    
                    if (usuario != null) {
                        _uiState.update { it.copy(
                            nombre = usuario.nombre,
                            selectedRole = usuario.role,
                            familyGroupId = usuario.familyGroupId.ifBlank { null },
                            authStatus = AuthStatus.Success(usuario, "Bienvenido de nuevo")
                        ) }
                    } else {
                        _uiState.update { it.copy(authStatus = AuthStatus.Idle) }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error al revisar sesión", e)
                    _uiState.update { it.copy(authStatus = AuthStatus.Idle) }
                }
            }
        }
    }

    fun resetAuthStatus() {
        _uiState.update { it.copy(authStatus = AuthStatus.Idle) }
    }

    fun cerrarSesion() {
        AuthRepository.cerrarSesion()
        _uiState.value = AuthUiState()
    }
}

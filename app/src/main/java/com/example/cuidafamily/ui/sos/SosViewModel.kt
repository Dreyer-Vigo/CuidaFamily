package com.example.cuidafamily.ui.sos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.SosAlert
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.repository.SosRepository
import com.example.cuidafamily.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// TODO: migrar a FCM + Cloud Functions para notificación push real entre dispositivos cerrados.

data class SosUiState(
    val activeAlerts: List<SosAlert> = emptyList(),
    val incomingAlert: SosAlert? = null,
    val showConfirmDialog: Boolean = false,
    val isTriggering: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SosViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private var currentGroupId: String? = null
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var listeningJob: Job? = null
    private var lastNotifiedAlertId: String? = null

    /**
     * Inicia la escucha en tiempo real de las alertas de emergencia del grupo familiar.
     */
    fun iniciarEscucha(
        familyGroupId: String,
        userId: String,
        userName: String,
        context: Context? = null
    ) {
        if (familyGroupId.isBlank()) return

        if (familyGroupId == currentGroupId && userId == currentUserId && listeningJob?.isActive == true) {
            return
        }

        currentGroupId = familyGroupId
        currentUserId = userId
        currentUserName = userName

        listeningJob?.cancel()
        listeningJob = viewModelScope.launch {
            SosRepository.escucharAlertasActivas(familyGroupId).collectLatest { alerts ->
                // Alerta activa disparada por otro miembro del grupo que aún no ha sido atendida
                val incoming = alerts.firstOrNull { it.disparadoPorUserId != currentUserId && !it.atendida }

                if (incoming != null && incoming.id != lastNotifiedAlertId && context != null) {
                    lastNotifiedAlertId = incoming.id
                    NotificationHelper.mostrarNotificacionSos(
                        context = context,
                        alertId = incoming.id,
                        nombreUsuario = incoming.disparadoPorNombre,
                        fecha = incoming.fecha
                    )
                    NotificationHelper.dispararVibracionSos(context)
                }

                _uiState.update {
                    it.copy(
                        activeAlerts = alerts,
                        incomingAlert = incoming
                    )
                }
            }
        }
    }

    fun solicitarConfirmacionSos() {
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun cancelarConfirmacionSos() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    fun dispararAlerta(familyGroupId: String, userId: String, userName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTriggering = true, showConfirmDialog = false) }
            val result = SosRepository.dispararAlerta(familyGroupId, userId, userName)
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isTriggering = false,
                            successMessage = "¡Alerta SOS enviada a todos los familiares del grupo!"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isTriggering = false,
                            errorMessage = result.exception.message ?: "No se pudo enviar la alerta SOS"
                        )
                    }
                }
            }
        }
    }

    fun marcarComoAtendida(alert: SosAlert, userId: String, userName: String) {
        viewModelScope.launch {
            val result = SosRepository.marcarComoAtendida(
                familyGroupId = alert.familyGroupId,
                alertId = alert.id,
                userId = userId,
                nombre = userName
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            incomingAlert = if (it.incomingAlert?.id == alert.id) null else it.incomingAlert,
                            successMessage = "Alerta marcada como atendida."
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.exception.message ?: "Error al marcar alerta como atendida")
                    }
                }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

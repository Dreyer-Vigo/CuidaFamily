package com.example.cuidafamily.ui.calendar

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.CalendarEvent
import com.example.cuidafamily.model.EstadoConfirmacion
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.TipoEvento
import com.example.cuidafamily.repository.CalendarRepository
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.Calendar
import java.util.UUID

data class CalendarUiState(
    val anioVisible: Int = Calendar.getInstance().get(Calendar.YEAR),
    val mesVisible: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val diaSeleccionado: String = String.format(java.util.Locale.US, "%04d-%02d-%02d", 
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH) + 1,
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    ),
    val eventosDelMesMap: Map<String, List<CalendarEvent>> = emptyMap(),
    val eventosDelMesLista: List<CalendarEvent> = emptyList(),
    val eventosDelDiaSeleccionado: List<CalendarEvent> = emptyList(),
    val userRole: Role = Role.CUIDADOR_EXTERNO,
    val mensajeError: String? = null
) {
    val canWrite: Boolean
        get() = userRole == Role.ADMINISTRADOR_FAMILIAR || userRole == Role.COLABORADOR
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var currentFamilyGroupId: String = ""
    private var currentUserId: String = ""
    private var refreshJob: Job? = null

    fun iniciar(userId: String, familyGroupId: String, role: Role = Role.CUIDADOR_EXTERNO) {
        if (familyGroupId.isBlank() || userId.isBlank()) return

        if (familyGroupId == currentFamilyGroupId && userId == currentUserId && refreshJob?.isActive == true) {
            return
        }

        Log.d("Sesion", "Reiniciando listeners para uid=$userId grupo=$familyGroupId en CalendarViewModel")

        limpiar()
        currentFamilyGroupId = familyGroupId
        currentUserId = userId
        _uiState.update { it.copy(userRole = role) }
        refrescarEventos()
    }

    fun inicializarCalendario(familyGroupId: String, role: Role, userId: String = "MOCK_USER_ID") {
        val uid = if (userId != "MOCK_USER_ID") userId else (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")
        iniciar(uid, familyGroupId, role)
    }

    fun limpiar() {
        refreshJob?.cancel()
        refreshJob = null
        currentFamilyGroupId = ""
        currentUserId = ""
        CalendarRepository.limpiarMemoria()
        _uiState.value = CalendarUiState()
    }

    fun cambiarMes(avanzar: Boolean) {
        _uiState.update { state ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, state.anioVisible)
                set(Calendar.MONTH, state.mesVisible - 1)
                add(Calendar.MONTH, if (avanzar) 1 else -1)
            }
            state.copy(
                anioVisible = cal.get(Calendar.YEAR),
                mesVisible = cal.get(Calendar.MONTH) + 1
            )
        }
        refrescarEventos()
    }

    fun seleccionarDia(fecha: String) {
        _uiState.update { it.copy(diaSeleccionado = fecha) }
        refrescarEventos()
    }

    fun agregarEvento(
        titulo: String,
        tipo: TipoEvento,
        fecha: String,
        horaInicio: LocalTime,
        horaFin: LocalTime,
        notas: String?,
        asignadoAUserId: String?,
        asignadoANombre: String?,
        asignadoARol: Role?,
        creadoPorUserId: String,
        lugar: String = "",
        rolAsignado: String = "",
        fotoUrl: String = ""
    ) {
        if (!_uiState.value.canWrite) return

        viewModelScope.launch {
            val eventId = UUID.randomUUID().toString()
            val nuevoEvento = CalendarEvent(
                id = eventId,
                titulo = titulo,
                tipo = tipo,
                fecha = fecha,
                horaInicio = horaInicio,
                horaFin = horaFin,
                asignadoAUserId = asignadoAUserId,
                asignadoANombre = asignadoANombre,
                asignadoARol = asignadoARol,
                creadoPorUserId = creadoPorUserId,
                notas = notas,
                completado = false,
                familyGroupId = currentFamilyGroupId,
                confirmacionAsistencia = EstadoConfirmacion.PENDIENTE,
                lugar = lugar,
                rolAsignado = rolAsignado,
                fotoUrl = fotoUrl
            )
            val result = CalendarRepository.crearEvento(nuevoEvento)
            
            if (result is Result.Success) {
                if (asignadoAUserId == currentUserId) {
                    NotificationHelper.programarRecordatorioEvento(
                        getApplication(), eventId, titulo, fecha, horaInicio.toString()
                    )
                }
            } else if (result is Result.Error) {
                _uiState.update { it.copy(mensajeError = result.exception.message) }
            }
        }
    }

    fun modificarEvento(event: CalendarEvent) {
        if (!_uiState.value.canWrite) return

        viewModelScope.launch {
            val result = CalendarRepository.editarEvento(event)
            
            if (result is Result.Success) {
                if (event.asignadoAUserId == currentUserId) {
                    NotificationHelper.programarRecordatorioEvento(
                        getApplication(), event.id, event.titulo, event.fecha, event.horaInicio.toString()
                    )
                } else {
                    NotificationHelper.cancelarRecordatorioEvento(getApplication(), event.id)
                }
            } else if (result is Result.Error) {
                _uiState.update { it.copy(mensajeError = result.exception.message) }
            }
        }
    }

    fun reasignarEvento(eventId: String, nuevoUserId: String, nuevoNombre: String, nuevoRol: Role?) {
        viewModelScope.launch {
            val result = CalendarRepository.reasignarEvento(currentFamilyGroupId, eventId, nuevoUserId, nuevoNombre, nuevoRol)
            if (result is Result.Error) {
                _uiState.update { it.copy(mensajeError = result.exception.message) }
            }
        }
    }

    fun borrarEvento(eventId: String) {
        if (!_uiState.value.canWrite) return

        viewModelScope.launch {
            val result = CalendarRepository.eliminarEvento(currentFamilyGroupId, eventId)
            if (result is Result.Success) {
                NotificationHelper.cancelarRecordatorioEvento(getApplication(), eventId)
            } else if (result is Result.Error) {
                _uiState.update { it.copy(mensajeError = result.exception.message) }
            }
        }
    }

    fun completarEvento(eventId: String) {
        viewModelScope.launch {
            val result = CalendarRepository.marcarComoCompletado(currentFamilyGroupId, eventId)
            if (result is Result.Success) {
                NotificationHelper.cancelarRecordatorioEvento(getApplication(), eventId)
            } else if (result is Result.Error) {
                _uiState.update { it.copy(mensajeError = result.exception.message) }
            }
        }
    }

    fun responderAsistencia(eventId: String, estado: EstadoConfirmacion, motivo: String = "") {
        viewModelScope.launch {
            val result = CalendarRepository.actualizarConfirmacionAsistencia(currentFamilyGroupId, eventId, estado, motivo)
            if (result is Result.Error) {
                _uiState.update { it.copy(mensajeError = result.exception.message) }
            }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(mensajeError = null) }
    }

    private fun refrescarEventos() {
        if (currentFamilyGroupId.isBlank()) return

        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val state = _uiState.value
            
            kotlinx.coroutines.flow.combine(
                CalendarRepository.obtenerEventosDelMesRealTime(currentFamilyGroupId, state.mesVisible, state.anioVisible),
                CalendarRepository.obtenerEventosDelDiaRealTime(currentFamilyGroupId, state.diaSeleccionado)
            ) { listaMes, listaDia ->
                _uiState.update { 
                    it.copy(
                        eventosDelMesMap = listaMes.groupBy { ev -> ev.fecha },
                        eventosDelMesLista = listaMes,
                        eventosDelDiaSeleccionado = listaDia
                    )
                }
            }.collect {}
        }
    }
}

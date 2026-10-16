package com.example.cuidafamily.ui.turnos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.EstadoConfirmacion
import com.example.cuidafamily.model.EstadoTurno
import com.example.cuidafamily.model.Turno
import com.example.cuidafamily.model.TurnoCheckIn
import com.example.cuidafamily.repository.AuthRepository
import com.example.cuidafamily.repository.GroupMemberInfo
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.repository.TurnoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DiaSemanaItem(
    val fechaStr: String,
    val nombreDia: String,
    val numeroDiaMes: Int,
    val dayOfWeekNum: Int,
    val esHoy: Boolean
)

data class TurnoConEstado(
    val turno: Turno,
    val fechaStr: String,
    val estado: EstadoTurno,
    val checkIn: TurnoCheckIn?
)

data class TurnoUiState(
    val todosLosTurnos: List<Turno> = emptyList(),
    val turnosActivos: List<Turno> = emptyList(),
    val turnosCanceladosAvisos: List<Turno> = emptyList(),
    val checkInsHoy: List<TurnoCheckIn> = emptyList(),
    val miembrosGrupo: List<GroupMemberInfo> = emptyList(),
    val domicilioLat: Double? = null,
    val domicilioLng: Double? = null,
    val offsetSemanas: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class TurnoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TurnoUiState())
    val uiState: StateFlow<TurnoUiState> = _uiState.asStateFlow()

    private var currentGroupId: String? = null
    private var currentUserId: String? = null
    private var listeningJob: Job? = null

    fun iniciar(userId: String, familyGroupId: String) {
        if (familyGroupId.isBlank() || userId.isBlank()) return

        if (familyGroupId == currentGroupId && userId == currentUserId && listeningJob?.isActive == true) {
            return
        }

        Log.d("Sesion", "Reiniciando listeners para uid=$userId grupo=$familyGroupId en TurnoViewModel")

        limpiar()
        currentGroupId = familyGroupId
        currentUserId = userId
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val miembrosRes = AuthRepository.obtenerMiembrosDelGrupo(familyGroupId)
            if (miembrosRes is Result.Success) {
                _uiState.update { it.copy(miembrosGrupo = miembrosRes.data) }
            }

            val grupo = AuthRepository.obtenerGrupoPorId(familyGroupId)
            if (grupo != null) {
                _uiState.update {
                    it.copy(
                        domicilioLat = grupo.domicilioLat,
                        domicilioLng = grupo.domicilioLng
                    )
                }
            }
        }

        listeningJob = viewModelScope.launch {
            combine(
                TurnoRepository.obtenerTodosLosTurnos(familyGroupId),
                TurnoRepository.obtenerCheckInsDeHoy(familyGroupId)
            ) { turnos, checkIns ->
                Pair(turnos, checkIns)
            }.collectLatest { (turnos, checkIns) ->
                val activos = turnos.filter { it.activo }
                val canceladosAvisos = turnos.filter { !it.activo && it.cuidadorUserId == userId && it.avisoParaCuidador.isNotBlank() }

                val diasSemanaActuales = getDiasDeLaSemanaActual(_uiState.value.offsetSemanas)
                val m = activos.count { turno ->
                    diasSemanaActuales.any { dia -> turno.aplicaEnFecha(dia.fechaStr) }
                }
                val p = activos.count { turno ->
                    turno.cuidadorUserId == userId && turno.confirmacion == EstadoConfirmacion.PENDIENTE && !turno.esVencido()
                }

                Log.d("TurnoViewModel", "Recibidos de Firestore: ${turnos.size} turnos; aplican a la semana visible: $m; pendientes del usuario: $p")

                _uiState.update {
                    it.copy(
                        todosLosTurnos = turnos,
                        turnosActivos = activos,
                        turnosCanceladosAvisos = canceladosAvisos,
                        checkInsHoy = checkIns,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun cargarDatos(familyGroupId: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        iniciar(uid, familyGroupId)
    }

    fun limpiar() {
        listeningJob?.cancel()
        listeningJob = null
        currentGroupId = null
        currentUserId = null
        _uiState.value = TurnoUiState()
    }

    fun cambiarSemana(incremento: Int) {
        _uiState.update { it.copy(offsetSemanas = it.offsetSemanas + incremento) }
    }

    fun resetearSemana() {
        _uiState.update { it.copy(offsetSemanas = 0) }
    }

    fun getDiasDeLaSemanaActual(offsetSemanas: Int = _uiState.value.offsetSemanas): List<DiaSemanaItem> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.WEEK_OF_YEAR, offsetSemanas)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val list = mutableListOf<DiaSemanaItem>()
        val sdfFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfNombre = SimpleDateFormat("EEEE", Locale("es", "ES"))

        for (i in 0..6) {
            val date = cal.time
            val fechaStr = sdfFecha.format(date)
            val nombreDia = sdfNombre.format(date).replaceFirstChar { it.uppercase() }
            val numDia = cal.get(Calendar.DAY_OF_MONTH)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            list.add(
                DiaSemanaItem(
                    fechaStr = fechaStr,
                    nombreDia = nombreDia,
                    numeroDiaMes = numDia,
                    dayOfWeekNum = dayOfWeek,
                    esHoy = (fechaStr == hoyStr)
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return list
    }

    fun obtenerTurnosParaDia(diaItem: DiaSemanaItem): List<TurnoConEstado> {
        val state = _uiState.value
        val turnosDelDia = state.turnosActivos.filter { turno ->
            turno.aplicaEnFecha(diaItem.fechaStr)
        }

        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val horaActualStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        return turnosDelDia.map { turno ->
            val checkIn = state.checkInsHoy.firstOrNull { it.turnoId == turno.id && it.fecha == diaItem.fechaStr }

            val estado = when {
                diaItem.fechaStr < hoyStr -> EstadoTurno.COMPLETADO
                diaItem.fechaStr > hoyStr -> EstadoTurno.PROGRAMADO
                else -> {
                    if (checkIn != null) {
                        EstadoTurno.COMPLETADO
                    } else if (horaActualStr >= turno.horaInicio && horaActualStr <= turno.horaFin) {
                        EstadoTurno.EN_CURSO
                    } else if (horaActualStr > turno.horaFin) {
                        EstadoTurno.COMPLETADO
                    } else {
                        EstadoTurno.PROGRAMADO
                    }
                }
            }

            TurnoConEstado(
                turno = turno,
                fechaStr = diaItem.fechaStr,
                estado = estado,
                checkIn = checkIn
            )
        }
    }

    fun responderConfirmacionTurno(
        familyGroupId: String,
        turnoId: String,
        estado: EstadoConfirmacion,
        motivo: String = ""
    ) {
        viewModelScope.launch {
            val result = TurnoRepository.actualizarConfirmacionTurno(familyGroupId, turnoId, estado, motivo)
            when (result) {
                is Result.Success -> {
                    val msg = if (estado == EstadoConfirmacion.CONFIRMADO) "¡Turno confirmado!" else "Respuesta enviada: No disponible para el turno."
                    _uiState.update { it.copy(successMessage = msg) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message ?: "Error al responder al turno.") }
                }
            }
        }
    }

    fun reasignarTurno(
        familyGroupId: String,
        turnoId: String,
        nuevoCuidadorUserId: String,
        nuevoCuidadorNombre: String,
        nuevoCuidadorTelefono: String
    ) {
        viewModelScope.launch {
            val result = TurnoRepository.reasignarTurno(familyGroupId, turnoId, nuevoCuidadorUserId, nuevoCuidadorNombre, nuevoCuidadorTelefono)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = "Turno reasignado a $nuevoCuidadorNombre.") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message ?: "Error al reasignar turno.") }
                }
            }
        }
    }

    fun marcarRespuestaVista(familyGroupId: String, turnoId: String) {
        viewModelScope.launch {
            TurnoRepository.marcarRespuestaVista(familyGroupId, turnoId)
        }
    }

    fun marcarTodasRespuestasVistas(familyGroupId: String) {
        viewModelScope.launch {
            TurnoRepository.marcarTodasRespuestasVistas(familyGroupId)
        }
    }

    fun borrarTurnoDefinitivo(familyGroupId: String, turnoId: String) {
        viewModelScope.launch {
            TurnoRepository.borrarTurnoDefinitivo(familyGroupId, turnoId)
        }
    }

    fun registrarCheckIn(
        familyGroupId: String,
        turnoId: String,
        cuidadorUserId: String,
        latActual: Double,
        lngActual: Double
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = TurnoRepository.registrarCheckIn(familyGroupId, turnoId, cuidadorUserId, latActual, lngActual)
            when (result) {
                is Result.Success -> {
                    val checkIn = result.data
                    val msg = if (checkIn.ubicacionValida)
                        "✅ Entrada registrada y validada correctamente por ubicación GPS."
                    else
                        "⚠️ Entrada registrada pero fuera del rango de 150m esperado."
                    _uiState.update { it.copy(isSaving = false, successMessage = msg) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.exception.message ?: "Error al registrar entrada.") }
                }
            }
        }
    }

    fun configurarDomicilio(familyGroupId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = TurnoRepository.configurarDomicilio(familyGroupId, lat, lng)
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            domicilioLat = lat,
                            domicilioLng = lng,
                            successMessage = "¡Domicilio de referencia configurado con éxito!"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.exception.message ?: "Error al configurar domicilio.") }
                }
            }
        }
    }

    fun crearTurno(
        familyGroupId: String,
        cuidadorUserId: String,
        cuidadorNombre: String,
        cuidadorTelefono: String,
        diasSemana: List<Int>,
        horaInicio: String,
        horaFin: String,
        tareas: List<String>,
        creadoPorUserId: String,
        fechaInicio: Long = 0L,
        fechaFin: Long? = null
    ) {
        if (cuidadorUserId.isBlank() || diasSemana.isEmpty() || horaInicio.isBlank() || horaFin.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Selecciona un cuidador, días de la semana y horario.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val nuevoTurno = Turno(
                familyGroupId = familyGroupId,
                cuidadorUserId = cuidadorUserId,
                cuidadorNombre = cuidadorNombre,
                cuidadorTelefono = cuidadorTelefono.trim(),
                diasSemana = diasSemana,
                horaInicio = horaInicio,
                horaFin = horaFin,
                tareas = tareas,
                activo = true,
                creadoPorUserId = creadoPorUserId,
                confirmacion = EstadoConfirmacion.PENDIENTE,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin
            )

            val result = TurnoRepository.crearTurno(familyGroupId, nuevoTurno)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, successMessage = "¡Turno asignado exitosamente!") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.exception.message ?: "Error al crear turno.") }
                }
            }
        }
    }

    fun modificarTurno(familyGroupId: String, turnoAnterior: Turno?, nuevoTurno: Turno) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = TurnoRepository.editarTurno(familyGroupId, turnoAnterior, nuevoTurno)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, successMessage = "¡Turno actualizado correctamente!") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.exception.message ?: "Error al actualizar turno.") }
                }
            }
        }
    }

    fun desactivarTurno(familyGroupId: String, turnoId: String) {
        viewModelScope.launch {
            val result = TurnoRepository.eliminarTurno(familyGroupId, turnoId)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = "Turno desactivado.") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message ?: "Error al eliminar turno.") }
                }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

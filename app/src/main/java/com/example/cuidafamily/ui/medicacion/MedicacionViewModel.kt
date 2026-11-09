package com.example.cuidafamily.ui.medicacion

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.*
import com.example.cuidafamily.repository.MedicacionRepository
import com.example.cuidafamily.util.RecordatorioScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class MedicacionViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(MedicacionUiState())
    val uiState: StateFlow<MedicacionUiState> = _uiState.asStateFlow()

    private var escuchaMedsJob: Job? = null
    private var escuchaTomasJob: Job? = null

    fun iniciar(uid: String, familyGroupId: String) {
        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") return

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(uid).get().await()
                val nombre = userDoc.getString("nombre") ?: ""
                val roleStr = userDoc.getString("role") ?: "CUIDADOR_EXTERNO"
                val role = try { Role.valueOf(roleStr) } catch (e: Exception) { Role.CUIDADOR_EXTERNO }

                _uiState.update {
                    it.copy(
                        currentUserId = uid,
                        currentUserName = nombre,
                        currentUserRole = role,
                        familyGroupId = familyGroupId
                    )
                }

                iniciarEscuchas(familyGroupId, uid, role)
                RecordatorioScheduler.programarAlarmasProximas48H(getApplication(), familyGroupId, uid, role)
                refrescarDiagnostico()
            } catch (e: Exception) {
                Log.e("Medicacion", "Error al iniciar MedicacionViewModel: ${e.message}", e)
            }
        }
    }

    fun limpiar() {
        escuchaMedsJob?.cancel()
        escuchaTomasJob?.cancel()
        _uiState.value = MedicacionUiState()
    }

    private fun iniciarEscuchas(familyGroupId: String, uid: String, role: Role) {
        escuchaMedsJob?.cancel()
        escuchaTomasJob?.cancel()

        escuchaMedsJob = viewModelScope.launch {
            MedicacionRepository.escucharMedicamentos(familyGroupId).collect { meds ->
                val activos = meds.count { it.activo }
                val state = _uiState.value
                val fechaStr = state.fechaActual.toString()
                val dosisHoy = calcularDosisDelDia(fechaStr, meds, state.tomas)

                Log.d("Medicacion", "uid=$uid grupo=$familyGroupId rol=$role | recibidos ${meds.size} medicamentos, activos=$activos, dosis de hoy=${dosisHoy.size}, tomas=${state.tomas.size}")

                _uiState.update {
                    it.copy(
                        medicamentos = meds,
                        dosisDelDia = dosisHoy,
                        alarmasProgramadasCount = dosisHoy.size
                    )
                }

                RecordatorioScheduler.programarAlarmasProximas48H(getApplication(), familyGroupId, uid, role)
            }
        }

        val desdeFecha = LocalDate.now(ZoneId.systemDefault()).minusMonths(1).toString()
        val hastaFecha = LocalDate.now(ZoneId.systemDefault()).plusMonths(1).toString()

        escuchaTomasJob = viewModelScope.launch {
            MedicacionRepository.escucharTomas(familyGroupId, desdeFecha, hastaFecha).collect { tomas ->
                val state = _uiState.value
                val fechaStr = state.fechaActual.toString()
                val dosisHoy = calcularDosisDelDia(fechaStr, state.medicamentos, tomas)

                _uiState.update {
                    it.copy(
                        tomas = tomas,
                        dosisDelDia = dosisHoy
                    )
                }
            }
        }
    }

    fun cambiarFecha(nuevaFecha: LocalDate) {
        _uiState.update { it.copy(fechaActual = nuevaFecha) }
        actualizarDosis()
    }

    private fun actualizarDosis() {
        val state = _uiState.value
        val fechaStr = state.fechaActual.toString()
        val listaDosis = calcularDosisDelDia(fechaStr, state.medicamentos, state.tomas)
        _uiState.update { it.copy(dosisDelDia = listaDosis) }
    }

    private fun calcularDosisDelDia(
        fechaStr: String,
        medicamentos: List<Medicamento>,
        tomas: List<TomaMedicacion>
    ): List<DosisItem> {
        val fechaLocal = try {
            LocalDate.parse(fechaStr)
        } catch (e: Exception) {
            LocalDate.now(ZoneId.systemDefault())
        }

        val calDayOfWeek = when (fechaLocal.dayOfWeek) {
            DayOfWeek.MONDAY -> Calendar.MONDAY
            DayOfWeek.TUESDAY -> Calendar.TUESDAY
            DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
            DayOfWeek.THURSDAY -> Calendar.THURSDAY
            DayOfWeek.FRIDAY -> Calendar.FRIDAY
            DayOfWeek.SATURDAY -> Calendar.SATURDAY
            DayOfWeek.SUNDAY -> Calendar.SUNDAY
        }

        val yyyyMMdd = fechaStr.replace("-", "")
        val tomasMap = tomas.associateBy { it.id }
        val resultado = mutableListOf<DosisItem>()
        val nowMs = System.currentTimeMillis()

        for (med in medicamentos) {
            if (!med.activo) continue

            if (med.fechaInicio.isNotBlank()) {
                try {
                    val inicio = LocalDate.parse(med.fechaInicio)
                    if (fechaLocal.isBefore(inicio)) continue
                } catch (e: Exception) {
                    if (fechaStr < med.fechaInicio) continue
                }
            }

            if (med.fechaFin != null && med.fechaFin.isNotBlank()) {
                try {
                    val fin = LocalDate.parse(med.fechaFin)
                    if (fechaLocal.isAfter(fin)) continue
                } catch (e: Exception) {
                    if (fechaStr > med.fechaFin) continue
                }
            }

            if (med.diasSemana.isNotEmpty() && !med.diasSemana.contains(calDayOfWeek)) continue

            for (horario in med.horarios) {
                val hhmm = horario.replace(":", "")
                val deterministaId = "${med.id}_${yyyyMMdd}_$hhmm"
                val tomaGuardada = tomasMap[deterministaId]

                val estadoCalculado = if (tomaGuardada != null) {
                    when (tomaGuardada.estado) {
                        EstadoToma.CONFIRMADA -> EstadoDosis.CONFIRMADA
                        EstadoToma.OMITIDA -> EstadoDosis.OMITIDA
                    }
                } else {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        val scheduledDate = sdf.parse("$fechaStr $horario")
                        if (scheduledDate != null) {
                            val scheduledMs = scheduledDate.time
                            val diffMinutes = (nowMs - scheduledMs) / (60 * 1000L)
                            if (diffMinutes > 60) EstadoDosis.ATRASADA else EstadoDosis.PENDIENTE
                        } else {
                            EstadoDosis.PENDIENTE
                        }
                    } catch (e: Exception) {
                        EstadoDosis.PENDIENTE
                    }
                }

                resultado.add(
                    DosisItem(
                        medicamentoId = med.id,
                        medicamentoNombre = med.nombre,
                        dosis = med.dosis,
                        hora = horario,
                        fecha = fechaStr,
                        indicaciones = med.indicaciones,
                        estadoCalculado = estadoCalculado,
                        tomaGuardada = tomaGuardada,
                        deterministaId = deterministaId
                    )
                )
            }
        }

        return resultado.sortedBy { it.hora }
    }

    fun crearOEditarMedicamento(medicamento: Medicamento, onComplete: (Boolean) -> Unit) {
        val fgId = _uiState.value.familyGroupId
        if (fgId.isBlank()) {
            onComplete(false)
            return
        }

        val medFinal = if (medicamento.id.isBlank()) {
            medicamento.copy(
                creadoPorUserId = _uiState.value.currentUserId,
                creadoPorNombre = _uiState.value.currentUserName
            )
        } else {
            medicamento
        }

        viewModelScope.launch {
            val res = MedicacionRepository.crearOEditarMedicamento(fgId, medFinal)
            if (res is com.example.cuidafamily.repository.Result.Success) {
                RecordatorioScheduler.programarAlarmasProximas48H(getApplication(), fgId, _uiState.value.currentUserId, _uiState.value.currentUserRole)
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun desactivarMedicamento(medicamentoId: String, onComplete: (Boolean) -> Unit) {
        val fgId = _uiState.value.familyGroupId
        if (fgId.isBlank()) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            val res = MedicacionRepository.desactivarMedicamento(fgId, medicamentoId)
            if (res is com.example.cuidafamily.repository.Result.Success) {
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun confirmarToma(dosisItem: DosisItem, nota: String = "", onComplete: (Boolean) -> Unit) {
        val state = _uiState.value
        if (state.familyGroupId.isBlank()) {
            onComplete(false)
            return
        }

        RecordatorioScheduler.cancelarNotificacionesYAlarmasDosis(getApplication(), dosisItem.deterministaId)

        val toma = TomaMedicacion(
            id = dosisItem.deterministaId,
            familyGroupId = state.familyGroupId,
            medicamentoId = dosisItem.medicamentoId,
            medicamentoNombre = dosisItem.medicamentoNombre,
            dosis = dosisItem.dosis,
            fecha = dosisItem.fecha,
            horaProgramada = dosisItem.hora,
            estado = EstadoToma.CONFIRMADA,
            confirmadoPorUserId = state.currentUserId,
            confirmadoPorNombre = state.currentUserName,
            confirmadoPorRol = state.currentUserRole.name,
            horaConfirmacion = System.currentTimeMillis(),
            nota = nota
        )

        viewModelScope.launch {
            val res = MedicacionRepository.confirmarToma(state.familyGroupId, toma)
            if (res is com.example.cuidafamily.repository.Result.Success) {
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun marcarOmitida(dosisItem: DosisItem, nota: String, onComplete: (Boolean) -> Unit) {
        val state = _uiState.value
        if (state.familyGroupId.isBlank()) {
            onComplete(false)
            return
        }

        RecordatorioScheduler.cancelarNotificacionesYAlarmasDosis(getApplication(), dosisItem.deterministaId)

        val toma = TomaMedicacion(
            id = dosisItem.deterministaId,
            familyGroupId = state.familyGroupId,
            medicamentoId = dosisItem.medicamentoId,
            medicamentoNombre = dosisItem.medicamentoNombre,
            dosis = dosisItem.dosis,
            fecha = dosisItem.fecha,
            horaProgramada = dosisItem.hora,
            estado = EstadoToma.OMITIDA,
            confirmadoPorUserId = state.currentUserId,
            confirmadoPorNombre = state.currentUserName,
            confirmadoPorRol = state.currentUserRole.name,
            horaConfirmacion = System.currentTimeMillis(),
            nota = nota
        )

        viewModelScope.launch {
            val res = MedicacionRepository.marcarOmitida(state.familyGroupId, toma)
            if (res is com.example.cuidafamily.repository.Result.Success) {
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun refrescarDiagnostico() {
        val context = getApplication<Application>()
        val diag = RecordatorioScheduler.obtenerEstadoDiagnostico(context)
        _uiState.update { it.copy(diagnostico = diag) }
    }

    fun setPersonalRemindersEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        RecordatorioScheduler.setPersonalRemindersEnabled(context, enabled)
        val state = _uiState.value
        RecordatorioScheduler.programarAlarmasProximas48H(context, state.familyGroupId, state.currentUserId, state.currentUserRole)
        refrescarDiagnostico()
    }

    fun setDemoModeActive(active: Boolean) {
        val context = getApplication<Application>()
        RecordatorioScheduler.setDemoModeActive(context, active)
        val state = _uiState.value
        RecordatorioScheduler.programarAlarmasProximas48H(context, state.familyGroupId, state.currentUserId, state.currentUserRole)
        refrescarDiagnostico()
    }

    fun setAvisoPrevioActive(active: Boolean) {
        val context = getApplication<Application>()
        RecordatorioScheduler.setAvisoPrevioActive(context, active)
        val state = _uiState.value
        RecordatorioScheduler.programarAlarmasProximas48H(context, state.familyGroupId, state.currentUserId, state.currentUserRole)
        refrescarDiagnostico()
    }

    fun setAnticipacionMinutos(minutos: Int) {
        val context = getApplication<Application>()
        RecordatorioScheduler.setAnticipacionMinutos(context, minutos)
        val state = _uiState.value
        RecordatorioScheduler.programarAlarmasProximas48H(context, state.familyGroupId, state.currentUserId, state.currentUserRole)
        refrescarDiagnostico()
    }
}

data class MedicacionUiState(
    val currentUserId: String = "",
    val currentUserName: String = "",
    val currentUserRole: Role = Role.CUIDADOR_EXTERNO,
    val familyGroupId: String = "",
    val medicamentos: List<Medicamento> = emptyList(),
    val tomas: List<TomaMedicacion> = emptyList(),
    val fechaActual: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    val dosisDelDia: List<DosisItem> = emptyList(),
    val alarmasProgramadasCount: Int = 0,
    val filtroHistorialMedicamentoId: String? = null,
    val diagnostico: com.example.cuidafamily.util.DiagnosticInfo = com.example.cuidafamily.util.DiagnosticInfo()
)

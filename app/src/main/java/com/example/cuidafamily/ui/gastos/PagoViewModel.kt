package com.example.cuidafamily.ui.gastos

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.ExcepcionTurno
import com.example.cuidafamily.model.Pago
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.SolicitudIntercambio
import com.example.cuidafamily.model.Turno
import com.example.cuidafamily.model.TurnoCheckIn
import com.example.cuidafamily.repository.AuthRepository
import com.example.cuidafamily.repository.GroupMemberInfo
import com.example.cuidafamily.repository.PagoRepository
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.repository.TurnoRepository
import com.example.cuidafamily.util.CalculoPagos
import com.example.cuidafamily.util.Permisos
import com.example.cuidafamily.util.ResumenPagosCuidador
import com.example.cuidafamily.util.formatearSoles
import com.example.cuidafamily.util.sumarMontosPrecision
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class PagoUiState(
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val contarSinCheckIn: Boolean = true,
    val resumenesCuidadores: List<ResumenPagosCuidador> = emptyList(),
    val totalPagadoMes: Double = 0.0,
    val historialPagosMes: List<Pago> = emptyList(),
    val historialPagosFiltrados: List<Pago> = emptyList(),
    val todosLosPagos: List<Pago> = emptyList(),
    val miembrosCuidadores: List<GroupMemberInfo> = emptyList(),
    val turnos: List<Turno> = emptyList(),
    val historialFiltroMes: YearMonth? = null, // null = "Todos" (activo por defecto)
    val historialFiltroCuidadorId: String = "", // "" = "Todos los cuidadores"
    val highlightedPagoId: String? = null,
    val totalPagosLeidos: Int = 0, // N
    val totalPagosMesCount: Int = 0, // M
    val totalCuidadoresCount: Int = 0, // K
    val sinAcceso: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class PagoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PagoUiState())
    val uiState: StateFlow<PagoUiState> = _uiState.asStateFlow()

    private var combineJob: Job? = null
    private var lastDataHolder: DataHolder? = null
    private var currentGroupId: String = ""
    private var currentUserId: String = ""
    var currentRole: Role? = null
    var currentUserName: String = ""

    companion object {
        private const val PREFS_NAME = "cuida_family_pagos_prefs"
        private const val KEY_CONTAR_SIN_CHECKIN = "contar_sin_checkin_cumplidos"
    }

    /**
     * Requerimiento 5 & 9: Iniciar vista y cargar datos. Limpia sesión anterior al cambiar de cuenta.
     */
    fun iniciar(familyGroupId: String, userId: String, userName: String, rol: Role?, context: Context) {
        val acceso = Permisos.puedeVerFinanzas(rol)
        Log.d("Pagos", "iniciar -> rol=$rol, acceso=$acceso, groupId=$familyGroupId, userId=$userId")

        if (!acceso) {
            _uiState.update { it.copy(sinAcceso = true, isLoading = false) }
            return
        }

        if (familyGroupId == currentGroupId && userId == currentUserId && combineJob?.isActive == true) {
            return
        }

        limpiar()
        currentGroupId = familyGroupId
        currentUserId = userId
        currentUserName = userName
        currentRole = rol

        // Leer preferencia guardada para 'contarSinCheckIn' (ACTIVADO por defecto)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val contarSinCheckInGuardado = prefs.getBoolean(KEY_CONTAR_SIN_CHECKIN, true)

        _uiState.update {
            it.copy(
                isLoading = true,
                sinAcceso = false,
                contarSinCheckIn = contarSinCheckInGuardado
            )
        }

        combineJob = viewModelScope.launch {
            // Requerimiento 1: Cargar miembros y filtrar ÚNICAMENTE los que tengan rol CUIDADOR_EXTERNO
            val resMiembros = AuthRepository.obtenerMiembrosDelGrupo(familyGroupId)
            val todosMiembros = if (resMiembros is Result.Success) resMiembros.data else emptyList()
            val miembrosCuidadores = todosMiembros.filter { it.member.role == Role.CUIDADOR_EXTERNO }

            // Requerimiento 1: Registro de Log obligatorio
            Log.d("Pagos", "Beneficiarios: ${miembrosCuidadores.map { it.member.nombre + '/' + it.member.subtipo }}, total miembros=${todosMiembros.size}")

            _uiState.update { it.copy(miembrosCuidadores = miembrosCuidadores) }

            // Escuchar en paralelo todos los flujos de datos
            combine(
                combine(
                    TurnoRepository.obtenerTodosLosTurnos(familyGroupId),
                    PagoRepository.escucharTarifas(familyGroupId, rol),
                    PagoRepository.escucharCheckIns(familyGroupId)
                ) { turnos, tarifas, checkIns -> Triple(turnos, tarifas, checkIns) },
                combine(
                    TurnoRepository.obtenerExcepcionesTurno(familyGroupId),
                    TurnoRepository.obtenerSolicitudesIntercambio(familyGroupId),
                    PagoRepository.escucharPagos(familyGroupId, rol)
                ) { excepciones, solicitudes, pagos -> Triple(excepciones, solicitudes, pagos) }
            ) { (turnos, tarifas, checkIns), (excepciones, solicitudes, pagos) ->
                DataHolder(turnos, tarifas, checkIns, excepciones, solicitudes, pagos)
            }.collect { data ->
                lastDataHolder = data
                recalcularEstado(data, miembrosCuidadores)
            }
        }
    }

    private data class DataHolder(
        val turnos: List<Turno>,
        val tarifas: Map<String, Double>,
        val checkIns: List<TurnoCheckIn>,
        val excepciones: List<ExcepcionTurno>,
        val solicitudes: List<SolicitudIntercambio>,
        val pagos: List<Pago>
    )

    private fun recalcularEstado(data: DataHolder, miembrosCuidadores: List<GroupMemberInfo>) {
        val targetYearMonth = _uiState.value.selectedYearMonth
        val contarSinCheckIn = _uiState.value.contarSinCheckIn

        val idsCuidadores = miembrosCuidadores.map { it.member.userId }.toSet()
        val listaResumenes = mutableListOf<ResumenPagosCuidador>()

        for (m in miembrosCuidadores) {
            val id = m.member.userId
            val nombre = m.userName
            val subtipo = m.member.subtipo.ifBlank { "Cuidador/a" }

            val resumen = CalculoPagos.calcularResumenCuidadorMes(
                cuidadorUserId = id,
                cuidadorNombre = nombre,
                subtipo = subtipo,
                targetYearMonth = targetYearMonth,
                turnos = data.turnos,
                tarifasMap = data.tarifas,
                checkIns = data.checkIns,
                excepciones = data.excepciones,
                solicitudes = data.solicitudes,
                pagos = data.pagos,
                contarSinCheckIn = contarSinCheckIn
            )

            Log.d("Pagos", "cuidador=${resumen.cuidadorUserId}, cumplidas=${resumen.turnosCumplidosCount}, horas=${resumen.totalHoras}, devengado=${resumen.devengado}, pagado=${resumen.pagado}, pendiente=${resumen.pendiente}")
            listaResumenes.add(resumen)
        }

        // Pagos pertenecientes a Cuidadores Externos
        val pagosDeCuidadores = data.pagos.filter { p -> idsCuidadores.contains(p.cuidadorUserId) }

        // Requerimiento 3: Imputación al mes según 'periodoFin' (o 'fecha')
        val targetMonthPrefix = targetYearMonth.toString()
        val historialMes = pagosDeCuidadores.filter { p ->
            val refFecha = p.periodoFin.ifBlank { p.fecha }
            refFecha.startsWith(targetMonthPrefix)
        }.sortedByDescending { it.fechaRegistro }

        val totalPagadoMes = sumarMontosPrecision(historialMes.map { it.monto })

        // Requerimiento 2: Historial independiente con filtros (Filtro Mes null="Todos" activo por defecto)
        val fMes = _uiState.value.historialFiltroMes
        val fCuidador = _uiState.value.historialFiltroCuidadorId

        val historialFiltrado = pagosDeCuidadores.filter { p ->
            val matchMes = if (fMes == null) true else {
                val refFecha = p.periodoFin.ifBlank { p.fecha }
                refFecha.startsWith(fMes.toString())
            }
            val matchCuidador = if (fCuidador.isBlank()) true else p.cuidadorUserId == fCuidador
            matchMes && matchCuidador
        }.sortedByDescending { it.fechaRegistro }

        _uiState.update {
            it.copy(
                resumenesCuidadores = listaResumenes.sortedBy { r -> r.cuidadorNombre },
                totalPagadoMes = totalPagadoMes,
                historialPagosMes = historialMes,
                historialPagosFiltrados = historialFiltrado,
                todosLosPagos = pagosDeCuidadores,
                turnos = data.turnos,
                totalPagosLeidos = data.pagos.size,
                totalPagosMesCount = historialMes.size,
                totalCuidadoresCount = miembrosCuidadores.size,
                isLoading = false
            )
        }
    }

    fun setMes(yearMonth: YearMonth) {
        _uiState.update { it.copy(selectedYearMonth = yearMonth) }
        repasarCalculo()
    }

    fun setHistorialFiltroMes(yearMonth: YearMonth?) {
        _uiState.update { it.copy(historialFiltroMes = yearMonth) }
        repasarCalculo()
    }

    fun setHistorialFiltroCuidadorId(cuidadorId: String) {
        _uiState.update { it.copy(historialFiltroCuidadorId = cuidadorId) }
        repasarCalculo()
    }

    fun setContarSinCheckIn(enabled: Boolean, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_CONTAR_SIN_CHECKIN, enabled).apply()
        _uiState.update { it.copy(contarSinCheckIn = enabled) }
        repasarCalculo()
    }

    private fun repasarCalculo() {
        val data = lastDataHolder
        if (data != null) {
            recalcularEstado(data, _uiState.value.miembrosCuidadores)
        }
    }

    /**
     * Requerimiento 1: Confirmación al guardar. Mueve el selector al mes de periodoFin, muestra Snackbar y resalta la tarjeta.
     */
    fun crearOEditarPago(pago: Pago) {
        if (!Permisos.puedeVerFinanzas(currentRole)) {
            _uiState.update { it.copy(errorMessage = "Sin acceso a finanzas.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val res = if (pago.id.isBlank()) {
                PagoRepository.crearPago(currentGroupId, pago, currentRole)
            } else {
                val updateRes = PagoRepository.editarPago(currentGroupId, pago, currentRole)
                if (updateRes is Result.Success) Result.Success(pago) else Result.Error(Exception("No se pudo editar el pago"))
            }

            when (res) {
                is Result.Success -> {
                    val guardado = res.data
                    val montoFmt = formatearSoles(guardado.monto)
                    val msg = "Pago de $montoFmt registrado a ${guardado.cuidadorNombre}"

                    // Determinar el mes de imputación (periodoFin o fecha)
                    val refFecha = guardado.periodoFin.ifBlank { guardado.fecha }
                    val targetMonth = try {
                        val date = LocalDate.parse(refFecha)
                        YearMonth.from(date)
                    } catch (e: Exception) {
                        _uiState.value.selectedYearMonth
                    }

                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = msg,
                            selectedYearMonth = targetMonth,
                            highlightedPagoId = guardado.id
                        )
                    }
                }
                is Result.Error -> {
                    val errText = res.exception.message ?: "Error al guardar el pago."
                    _uiState.update { it.copy(isSaving = false, errorMessage = errText) }
                }
            }
        }
    }

    fun eliminarPago(pago: Pago) {
        if (!Permisos.puedeVerFinanzas(currentRole)) {
            _uiState.update { it.copy(errorMessage = "Sin acceso a finanzas.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val res = PagoRepository.eliminarPago(currentGroupId, pago.id, currentRole)
            when (res) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, successMessage = "Pago eliminado correctamente.") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = res.exception.message ?: "No se pudo eliminar el pago.") }
                }
            }
        }
    }

    fun marcarCumplimientoOcurrencia(
        turnoId: String,
        fechaIso: String,
        cuidadorUserId: String,
        manual: Boolean,
        noCumplido: Boolean
    ) {
        if (!Permisos.puedeVerFinanzas(currentRole)) {
            _uiState.update { it.copy(errorMessage = "Sin acceso a finanzas.") }
            return
        }

        viewModelScope.launch {
            val res = PagoRepository.marcarCumplimientoOcurrencia(
                familyGroupId = currentGroupId,
                turnoId = turnoId,
                fechaIso = fechaIso,
                cuidadorUserId = cuidadorUserId,
                manual = manual,
                noCumplido = noCumplido,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                userRole = currentRole
            )
            when (res) {
                is Result.Success -> {
                    val msg = if (manual) "Ocurrencia marcada como Cumplida" else "Ocurrencia marcada como No Cumplida"
                    _uiState.update { it.copy(successMessage = msg) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = res.exception.message ?: "Error al actualizar estado del turno.") }
                }
            }
        }
    }

    fun guardarTarifaTurno(turnoId: String, tarifaPorHora: Double) {
        if (!Permisos.puedeVerFinanzas(currentRole)) return

        viewModelScope.launch {
            PagoRepository.guardarTarifa(currentGroupId, turnoId, tarifaPorHora, currentRole)
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun limpiarHighlight() {
        _uiState.update { it.copy(highlightedPagoId = null) }
    }

    /**
     * Requerimiento 5 & 9: Limpiar todo el estado al cambiar de cuenta.
     */
    fun limpiar() {
        combineJob?.cancel()
        combineJob = null
        lastDataHolder = null
        currentGroupId = ""
        currentUserId = ""
        currentUserName = ""
        currentRole = null
        _uiState.value = PagoUiState()
    }
}

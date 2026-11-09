package com.example.cuidafamily.ui.auditoria

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.AccionAuditoria
import com.example.cuidafamily.model.EntidadAuditoria
import com.example.cuidafamily.model.RegistroAuditoria
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.repository.AuditoriaRepository
import com.example.cuidafamily.repository.AuthRepository
import com.example.cuidafamily.repository.GroupMemberInfo
import com.example.cuidafamily.repository.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

data class AuditoriaUiState(
    val fechaDesde: LocalDate = LocalDate.now().minusDays(30),
    val fechaHasta: LocalDate = LocalDate.now(),
    val filtroEntidad: EntidadAuditoria? = null, // null = "Todos"
    val filtroAccion: AccionAuditoria? = null, // null = "Todas"
    val filtroUserId: String = "", // "" = "Todas las personas"
    val limiteFirestore: Int = 300,
    val todosLosRegistrosFirestore: List<RegistroAuditoria> = emptyList(),
    val registrosFiltrados: List<RegistroAuditoria> = emptyList(),
    val miembrosGrupo: List<GroupMemberInfo> = emptyList(),
    val totalMovimientos: Int = 0,
    val conteoPorAccion: Map<AccionAuditoria, Int> = emptyMap(),
    val sinAcceso: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuditoriaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuditoriaUiState())
    val uiState: StateFlow<AuditoriaUiState> = _uiState.asStateFlow()

    private var currentGroupId: String = ""
    private var cargarJob: Job? = null

    /**
     * Requerimiento 07: Iniciar vista y verificar permisos. Visible SOLO para ADMINISTRADOR_FAMILIAR.
     */
    fun iniciar(familyGroupId: String, userRole: Role?) {
        val esAdmin = userRole == Role.ADMINISTRADOR_FAMILIAR
        Log.d("Auditoria", "iniciar -> rol=$userRole, esAdmin=$esAdmin, groupId=$familyGroupId")

        if (!esAdmin) {
            _uiState.update { it.copy(sinAcceso = true, isLoading = false) }
            return
        }

        if (familyGroupId == currentGroupId && cargarJob?.isActive == true) {
            return
        }

        limpiar()
        currentGroupId = familyGroupId
        _uiState.update { it.copy(isLoading = true, sinAcceso = false) }

        viewModelScope.launch {
            val resMiembros = AuthRepository.obtenerMiembrosDelGrupo(familyGroupId)
            if (resMiembros is Result.Success) {
                _uiState.update { it.copy(miembrosGrupo = resMiembros.data) }
            }
            cargarMovimientos()
        }
    }

    fun cargarMovimientos() {
        if (currentGroupId.isBlank()) return

        cargarJob?.cancel()
        cargarJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val fDesde = _uiState.value.fechaDesde
            val fHasta = _uiState.value.fechaHasta
            val limite = _uiState.value.limiteFirestore

            val desdeMs = fDesde.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val hastaMs = fHasta.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

            val res = AuditoriaRepository.obtenerRegistros(currentGroupId, desdeMs, hastaMs, limite)
            when (res) {
                is Result.Success -> {
                    val rawList = res.data
                    _uiState.update { it.copy(todosLosRegistrosFirestore = rawList) }
                    aplicarFiltrosMemoria(rawList)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = res.exception.message ?: "No se pudieron cargar los movimientos de auditoría."
                        )
                    }
                }
            }
        }
    }

    private fun aplicarFiltrosMemoria(rawList: List<RegistroAuditoria>) {
        val state = _uiState.value
        val fEntidad = state.filtroEntidad
        val fAccion = state.filtroAccion
        val fUserId = state.filtroUserId

        val filtrados = rawList.filter { reg ->
            val matchEntidad = if (fEntidad == null) true else reg.entidad == fEntidad
            val matchAccion = if (fAccion == null) true else reg.accion == fAccion
            val matchUser = if (fUserId.isBlank()) true else reg.userId == fUserId
            matchEntidad && matchAccion && matchUser
        }.sortedByDescending { it.fecha }

        val conteo = filtrados.groupBy { it.accion }
            .mapValues { (_, items) -> items.size }

        val filtrosDesc = "Desde=$state.fechaDesde, Hasta=$state.fechaHasta, Entidad=$fEntidad, Accion=$fAccion, User=$fUserId"
        // Requerimiento 6: Log obligatorio
        Log.d("Auditoria", "Reporte: ${filtrados.size} registros, filtros=$filtrosDesc")

        _uiState.update {
            it.copy(
                registrosFiltrados = filtrados,
                totalMovimientos = filtrados.size,
                conteoPorAccion = conteo,
                isLoading = false
            )
        }
    }

    fun setFechaDesde(fecha: LocalDate) {
        _uiState.update { it.copy(fechaDesde = fecha) }
        cargarMovimientos()
    }

    fun setFechaHasta(fecha: LocalDate) {
        _uiState.update { it.copy(fechaHasta = fecha) }
        cargarMovimientos()
    }

    fun setFiltroEntidad(entidad: EntidadAuditoria?) {
        _uiState.update { it.copy(filtroEntidad = entidad) }
        aplicarFiltrosMemoria(_uiState.value.todosLosRegistrosFirestore)
    }

    fun setFiltroAccion(accion: AccionAuditoria?) {
        _uiState.update { it.copy(filtroAccion = accion) }
        aplicarFiltrosMemoria(_uiState.value.todosLosRegistrosFirestore)
    }

    fun setFiltroUserId(userId: String) {
        _uiState.update { it.copy(filtroUserId = userId) }
        aplicarFiltrosMemoria(_uiState.value.todosLosRegistrosFirestore)
    }

    fun cargarMas() {
        val nuevoLimite = _uiState.value.limiteFirestore + 300
        _uiState.update { it.copy(limiteFirestore = nuevoLimite) }
        cargarMovimientos()
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /**
     * Requerimiento 6: Limpiar estado al cambiar de cuenta.
     */
    fun limpiar() {
        cargarJob?.cancel()
        cargarJob = null
        currentGroupId = ""
        _uiState.value = AuditoriaUiState()
    }
}

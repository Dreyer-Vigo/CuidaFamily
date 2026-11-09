package com.example.cuidafamily.ui.gastos

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.Gasto
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.repository.GastoRepository
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.util.ComprobanteProcessResult
import com.example.cuidafamily.util.ComprobanteProcessor
import com.example.cuidafamily.util.Permisos
import com.example.cuidafamily.util.sumarMontosPrecision
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GastoUiState(
    val gastos: List<Gasto> = emptyList(),
    val totalGastos: Double = 0.0,
    val gastosPorCategoria: Map<String, Double> = emptyMap(),
    val sinAcceso: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isProcessingComprobante: Boolean = false,
    val comprobanteProcessingMsg: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class GastoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GastoUiState())
    val uiState: StateFlow<GastoUiState> = _uiState.asStateFlow()

    private var listeningJob: Job? = null
    private var currentGroupId: String = ""
    private var currentUserId: String = ""
    var currentRole: Role? = null

    fun iniciar(familyGroupId: String, userId: String, rol: Role?) {
        val acceso = Permisos.puedeVerFinanzas(rol)
        Log.d("Gastos", "rol=$rol, acceso=$acceso")

        if (!acceso) {
            _uiState.update { it.copy(sinAcceso = true, isLoading = false) }
            return
        }

        if (familyGroupId == currentGroupId && userId == currentUserId && listeningJob?.isActive == true) {
            return
        }

        limpiar()
        currentGroupId = familyGroupId
        currentUserId = userId
        currentRole = rol

        _uiState.update { it.copy(isLoading = true, sinAcceso = false) }

        listeningJob = viewModelScope.launch {
            GastoRepository.obtenerGastos(familyGroupId, rol).collectLatest { listaGastos ->
                val total = sumarMontosPrecision(listaGastos.map { it.monto })
                val porCat = listaGastos.groupBy { it.categoria.ifBlank { "General" } }
                    .mapValues { (_, items) -> sumarMontosPrecision(items.map { it.monto }) }

                _uiState.update {
                    it.copy(
                        gastos = listaGastos,
                        totalGastos = total,
                        gastosPorCategoria = porCat,
                        isLoading = false,
                        sinAcceso = false
                    )
                }
            }
        }
    }

    fun procesarComprobante(
        context: Context,
        uri: Uri,
        origen: String,
        onResult: (ComprobanteProcessResult) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingComprobante = true, comprobanteProcessingMsg = "Preparando comprobante…") }
            val result = ComprobanteProcessor.procesarComprobanteUri(context, uri, origen)
            _uiState.update { it.copy(isProcessingComprobante = false, comprobanteProcessingMsg = null) }

            if (result.errorLectura) {
                _uiState.update { it.copy(errorMessage = "No se pudo leer el archivo. Prueba con otra foto o con 'Tomar foto'.") }
            } else if (result.errorDemasiadoGrande) {
                _uiState.update { it.copy(errorMessage = "No se pudo compartir el comprobante con el grupo; solo quedará en este celular.") }
            }

            onResult(result)
        }
    }

    fun crearOEditarGasto(familyGroupId: String, gasto: Gasto, rol: Role?) {
        val acceso = Permisos.puedeVerFinanzas(rol)
        Log.d("Gastos", "rol=$rol, acceso=$acceso")

        if (!acceso) {
            _uiState.update { it.copy(errorMessage = "Sin acceso a finanzas.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val res = if (gasto.id.isBlank()) {
                GastoRepository.crearGasto(familyGroupId, gasto, rol)
            } else {
                GastoRepository.editarGasto(familyGroupId, gasto, rol)
            }

            when (res) {
                is Result.Success -> {
                    val msg = if (gasto.id.isBlank()) "¡Gasto registrado con éxito!" else "¡Gasto actualizado con éxito!"
                    _uiState.update { it.copy(isSaving = false, successMessage = msg) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = res.exception.message ?: "Error al guardar el gasto.") }
                }
            }
        }
    }

    fun eliminarGasto(familyGroupId: String, gasto: Gasto, userId: String, rol: Role?) {
        val acceso = Permisos.puedeVerFinanzas(rol)
        Log.d("Gastos", "rol=$rol, acceso=$acceso")

        if (!acceso) {
            _uiState.update { it.copy(errorMessage = "Sin acceso a finanzas.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val res = GastoRepository.eliminarGasto(familyGroupId, gasto, userId, rol)
            when (res) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, successMessage = "Gasto eliminado correctamente.") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = res.exception.message ?: "No se pudo eliminar el gasto.") }
                }
            }
        }
    }

    fun reclamarGastoComoMio(familyGroupId: String, gastoId: String, userId: String, userName: String, rol: Role?) {
        val acceso = Permisos.puedeVerFinanzas(rol)
        Log.d("Gastos", "rol=$rol, acceso=$acceso")

        if (!acceso) {
            _uiState.update { it.copy(errorMessage = "Sin acceso a finanzas.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val res = GastoRepository.reclamarGastoComoMio(familyGroupId, gastoId, userId, userName, rol)
            when (res) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, successMessage = "Gasto asignado a tu propiedad exitosamente.") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = res.exception.message ?: "No se pudo reclamar el gasto.") }
                }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun limpiar() {
        listeningJob?.cancel()
        listeningJob = null
        currentGroupId = ""
        currentUserId = ""
        currentRole = null
        _uiState.value = GastoUiState()
    }
}

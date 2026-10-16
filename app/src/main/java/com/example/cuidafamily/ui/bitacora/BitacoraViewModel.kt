package com.example.cuidafamily.ui.bitacora

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.BitacoraEntrada
import com.example.cuidafamily.model.EstadoAnimo
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.repository.BitacoraRepository
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class BitacoraState(
    val entradas: List<BitacoraEntrada> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val noVistasCount: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BitacoraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BitacoraState())
    val uiState: StateFlow<BitacoraState> = _uiState.asStateFlow()

    private var currentGroupId: String? = null
    private var currentUserId: String? = null
    private var listeningJob: Job? = null

    fun iniciar(userId: String, familyGroupId: String, context: Context? = null, patientNombre: String = "el adulto mayor") {
        if (familyGroupId.isBlank() || userId.isBlank()) return

        if (familyGroupId == currentGroupId && userId == currentUserId && listeningJob?.isActive == true) {
            return
        }

        Log.d("Sesion", "Reiniciando listeners para uid=$userId grupo=$familyGroupId en BitacoraViewModel")

        limpiar()
        currentGroupId = familyGroupId
        currentUserId = userId
        _uiState.update { it.copy(isLoading = true) }

        var isInitialLoad = true
        var previousEntryIds = setOf<String>()

        listeningJob = viewModelScope.launch {
            BitacoraRepository.obtenerEntradas(familyGroupId).collectLatest { nuevasEntradas ->
                Log.d("BitacoraViewModel", "Nueva lista recibida: ${nuevasEntradas.size} entradas")

                val newEntryIds = nuevasEntradas.map { it.id }.toSet()

                if (isInitialLoad) {
                    val recentOtherEntries = nuevasEntradas.filter { entry ->
                        entry.autorUserId != userId && isRecentEntry(entry.fecha, maxAgeMinutes = 1440)
                    }
                    recentOtherEntries.firstOrNull()?.let { entrada ->
                        if (context != null) {
                            val animoStr = if (entrada.estadoAnimo != EstadoAnimo.NINGUNO) "${entrada.estadoAnimo.emoji} ${entrada.estadoAnimo.etiqueta}" else ""
                            NotificationHelper.mostrarNotificacionBitacora(
                                context = context,
                                autorNombre = entrada.autorNombre,
                                patientNombre = patientNombre,
                                estadoAnimoTexto = animoStr,
                                notaTexto = entrada.nota
                            )
                        }
                    }
                    isInitialLoad = false
                } else {
                    val addedEntries = nuevasEntradas.filter { it.id !in previousEntryIds && it.autorUserId != userId }
                    for (entrada in addedEntries) {
                        if (context != null) {
                            val animoStr = if (entrada.estadoAnimo != EstadoAnimo.NINGUNO) "${entrada.estadoAnimo.emoji} ${entrada.estadoAnimo.etiqueta}" else ""
                            NotificationHelper.mostrarNotificacionBitacora(
                                context = context,
                                autorNombre = entrada.autorNombre,
                                patientNombre = patientNombre,
                                estadoAnimoTexto = animoStr,
                                notaTexto = entrada.nota
                            )
                        }
                    }
                }

                previousEntryIds = newEntryIds

                _uiState.update {
                    it.copy(
                        entradas = nuevasEntradas,
                        isLoading = false
                    )
                }

                if (userId.isNotBlank()) {
                    calcularNovedadesNoVistas(familyGroupId, userId, nuevasEntradas)
                }
            }
        }
    }

    fun cargarBitacora(
        familyGroupId: String,
        userId: String = "",
        context: Context? = null,
        patientNombre: String = "el adulto mayor"
    ) {
        iniciar(userId, familyGroupId, context, patientNombre)
    }

    fun calcularNovedadesNoVistas(familyGroupId: String, userId: String, listaActual: List<BitacoraEntrada> = _uiState.value.entradas) {
        if (familyGroupId.isBlank() || userId.isBlank()) return

        viewModelScope.launch {
            val ultimaVistaMs = BitacoraRepository.obtenerUltimaVistaBitacora(familyGroupId, userId)

            val unreadCount = listaActual.count { entrada ->
                if (entrada.autorUserId == userId) false
                else {
                    val entryDateMs = parseFechaToMillis(entrada.fecha)
                    entryDateMs > ultimaVistaMs
                }
            }

            _uiState.update { it.copy(noVistasCount = unreadCount) }
            Log.d("BitacoraViewModel", "Novedades no vistas calculadas para $userId: $unreadCount (Última vista: $ultimaVistaMs)")
        }
    }

    fun marcarBitacoraComoVista(familyGroupId: String, userId: String) {
        if (familyGroupId.isBlank() || userId.isBlank()) return

        viewModelScope.launch {
            BitacoraRepository.actualizarUltimaVistaBitacora(familyGroupId, userId)
            _uiState.update { it.copy(noVistasCount = 0) }
            Log.d("BitacoraViewModel", "Bitácora marcada como vista para $userId")
        }
    }

    private fun isRecentEntry(fechaIso: String, maxAgeMinutes: Long): Boolean {
        if (fechaIso.isBlank()) return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(fechaIso) ?: return false
            val diffMs = System.currentTimeMillis() - date.time
            diffMs in 0..(maxAgeMinutes * 60 * 1000)
        } catch (e: Exception) {
            true
        }
    }

    private fun parseFechaToMillis(fechaIso: String): Long {
        if (fechaIso.isBlank()) return 0L
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.parse(fechaIso)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun limpiar() {
        listeningJob?.cancel()
        listeningJob = null
        currentGroupId = null
        currentUserId = null
        _uiState.value = BitacoraState()
    }

    fun limpiarSesion() {
        limpiar()
    }

    fun crearPublicacion(
        familyGroupId: String,
        autorUserId: String,
        autorNombre: String,
        autorRol: Role,
        estadoAnimo: EstadoAnimo,
        nota: String,
        fotoUrl: String
    ) {
        if (nota.isBlank() && fotoUrl.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Agrega una nota o una foto para publicar.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val nuevaEntrada = BitacoraEntrada(
                familyGroupId = familyGroupId,
                autorUserId = autorUserId,
                autorNombre = autorNombre,
                autorRol = autorRol,
                estadoAnimo = estadoAnimo,
                nota = nota.trim(),
                fotoUrl = fotoUrl
            )

            val result = BitacoraRepository.crearEntrada(familyGroupId, nuevaEntrada)
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "¡Publicación agregada con éxito!"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.exception.message ?: "No se pudo crear la publicación."
                        )
                    }
                }
            }
        }
    }

    fun eliminarPublicacion(familyGroupId: String, entradaId: String) {
        viewModelScope.launch {
            val result = BitacoraRepository.eliminarEntrada(familyGroupId, entradaId)
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(successMessage = "Publicación eliminada correctamente.")
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.exception.message ?: "Error al eliminar publicación.")
                    }
                }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

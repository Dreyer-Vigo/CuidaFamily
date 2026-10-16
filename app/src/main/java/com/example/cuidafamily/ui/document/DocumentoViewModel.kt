package com.example.cuidafamily.ui.document

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.DocumentoMedico
import com.example.cuidafamily.model.TipoDocumento
import com.example.cuidafamily.repository.DocumentoRepository
import com.example.cuidafamily.repository.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// TODO: migrar a Firebase Storage para acceso compartido real entre dispositivos.

data class DocumentoUiState(
    val documentos: List<DocumentoMedico> = emptyList(),
    val filtroTipo: TipoDocumento? = null,
    val isLoading: Boolean = false,
    val isSubiendo: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val documentosFiltrados: List<DocumentoMedico>
        get() = if (filtroTipo == null) documentos else documentos.filter { it.tipo == filtroTipo }
}

class DocumentoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentoUiState())
    val uiState: StateFlow<DocumentoUiState> = _uiState.asStateFlow()

    private var currentGroupId: String? = null
    private var currentPatientId: String? = null
    private var listeningJob: Job? = null

    fun iniciar(familyGroupId: String, patientId: String) {
        if (familyGroupId.isBlank() || patientId.isBlank()) return

        if (familyGroupId == currentGroupId && patientId == currentPatientId && listeningJob?.isActive == true) {
            return
        }

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("Sesion", "Reiniciando listeners para uid=$uid grupo=$familyGroupId en DocumentoViewModel")

        limpiar()
        currentGroupId = familyGroupId
        currentPatientId = patientId
        _uiState.update { it.copy(isLoading = true) }

        listeningJob = viewModelScope.launch {
            DocumentoRepository.obtenerDocumentos(familyGroupId, patientId).collectLatest { lista ->
                _uiState.update {
                    it.copy(
                        documentos = lista,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun cargarDocumentos(familyGroupId: String, patientId: String) {
        iniciar(familyGroupId, patientId)
    }

    fun limpiar() {
        listeningJob?.cancel()
        listeningJob = null
        currentGroupId = null
        currentPatientId = null
        _uiState.value = DocumentoUiState()
    }

    fun setFiltroTipo(tipo: TipoDocumento?) {
        _uiState.update { it.copy(filtroTipo = tipo) }
    }

    fun subirDocumento(
        familyGroupId: String,
        patientId: String,
        tipo: TipoDocumento,
        nombre: String,
        fechaDocumento: String,
        uriLocal: String,
        mimeType: String,
        subidoPorUserId: String,
        subidoPorNombre: String
    ) {
        if (nombre.isBlank() || uriLocal.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un nombre y selecciona un archivo válido.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubiendo = true) }

            val nuevoDoc = DocumentoMedico(
                patientId = patientId,
                familyGroupId = familyGroupId,
                tipo = tipo,
                nombre = nombre.trim(),
                fechaDocumento = fechaDocumento.ifBlank { "Sin fecha" },
                subidoPorUserId = subidoPorUserId,
                subidoPorNombre = subidoPorNombre,
                uriLocal = uriLocal,
                mimeType = mimeType
            )

            val result = DocumentoRepository.subirDocumento(familyGroupId, patientId, nuevoDoc)
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubiendo = false,
                            successMessage = "¡Documento médico registrado exitosamente!"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubiendo = false,
                            errorMessage = result.exception.message ?: "No se pudo subir el documento."
                        )
                    }
                }
            }
        }
    }

    fun eliminarDocumento(familyGroupId: String, patientId: String, documentoId: String) {
        viewModelScope.launch {
            val result = DocumentoRepository.eliminarDocumento(familyGroupId, patientId, documentoId)
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(successMessage = "Documento médico eliminado correctamente.")
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.exception.message ?: "Error al eliminar documento.")
                    }
                }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

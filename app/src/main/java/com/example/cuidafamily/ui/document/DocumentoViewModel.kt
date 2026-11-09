package com.example.cuidafamily.ui.document

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.DocumentoMedico
import com.example.cuidafamily.model.Role
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

// TODO: migrar a Firebase Storage para compartir archivos completos sin límite de tamaño.

data class DocumentoUiState(
    val documentos: List<DocumentoMedico> = emptyList(),
    val filtroTipo: TipoDocumento? = null,
    val isLoading: Boolean = false,
    val isSubiendo: Boolean = false,
    val userRole: Role = Role.CUIDADOR_EXTERNO,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val documentosFiltrados: List<DocumentoMedico>
        get() = when {
            userRole == Role.CUIDADOR_EXTERNO -> documentos.filter { it.tipo == TipoDocumento.RECETA }
            filtroTipo == null -> documentos
            else -> documentos.filter { it.tipo == filtroTipo }
        }
}

class DocumentoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentoUiState())
    val uiState: StateFlow<DocumentoUiState> = _uiState.asStateFlow()

    private var currentGroupId: String? = null
    private var currentPatientId: String? = null
    private var listeningJob: Job? = null

    fun iniciar(familyGroupId: String, patientId: String, userRole: Role = Role.CUIDADOR_EXTERNO) {
        if (familyGroupId.isBlank() || patientId.isBlank()) return

        if (familyGroupId == currentGroupId && patientId == currentPatientId && listeningJob?.isActive == true && _uiState.value.userRole == userRole) {
            return
        }

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        Log.d("Sesion", "Reiniciando listeners para uid=$uid grupo=$familyGroupId en DocumentoViewModel")

        limpiar()
        currentGroupId = familyGroupId
        currentPatientId = patientId
        _uiState.update { it.copy(isLoading = true, userRole = userRole) }

        listeningJob = viewModelScope.launch {
            DocumentoRepository.obtenerDocumentos(familyGroupId, patientId).collectLatest { lista ->
                val listaMostrada = if (userRole == Role.CUIDADOR_EXTERNO) {
                    lista.filter { it.tipo == TipoDocumento.RECETA }
                } else {
                    lista
                }

                Log.d("Documentos", "rol=$userRole, mostrando ${listaMostrada.size} documentos (tipo filtrado=${_uiState.value.filtroTipo})")

                _uiState.update {
                    it.copy(
                        documentos = listaMostrada,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun cargarDocumentos(familyGroupId: String, patientId: String, userRole: Role = Role.CUIDADOR_EXTERNO) {
        iniciar(familyGroupId, patientId, userRole)
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
        subidoPorNombre: String,
        indicaciones: String = "",
        imagenesBase64: List<String> = emptyList(),
        paginasTotales: Int = 0,
        paginasCompartidas: Int = 0
    ) {
        if (nombre.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un nombre para el documento.") }
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
                mimeType = mimeType,
                indicaciones = indicaciones.trim(),
                imagenBase64 = imagenesBase64.firstOrNull() ?: "",
                imagenesBase64 = imagenesBase64,
                paginasTotales = paginasTotales,
                paginasCompartidas = paginasCompartidas
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

    fun compartirDocumentoBase64(
        familyGroupId: String,
        patientId: String,
        documentoId: String,
        imagenesBase64: List<String>,
        paginasTotales: Int,
        paginasCompartidas: Int
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubiendo = true) }
            val result = DocumentoRepository.actualizarImagenesBase64(
                familyGroupId,
                patientId,
                documentoId,
                imagenesBase64,
                paginasTotales,
                paginasCompartidas
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubiendo = false,
                            successMessage = "¡Copia del documento compartida con éxito ($paginasCompartidas de $paginasTotales páginas)!"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubiendo = false,
                            errorMessage = result.exception.message ?: "No se pudo compartir la copia del documento."
                        )
                    }
                }
            }
        }
    }

    fun actualizarIndicaciones(
        familyGroupId: String,
        patientId: String,
        documentoId: String,
        nuevasIndicaciones: String
    ) {
        viewModelScope.launch {
            val result = DocumentoRepository.actualizarIndicaciones(familyGroupId, patientId, documentoId, nuevasIndicaciones)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = "Indicaciones de la receta actualizadas.") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message ?: "Error al actualizar indicaciones.") }
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

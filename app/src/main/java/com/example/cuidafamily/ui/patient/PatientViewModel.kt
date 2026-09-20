package com.example.cuidafamily.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cuidafamily.model.Allergy
import com.example.cuidafamily.model.Patient
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.WeightRecord
import com.example.cuidafamily.repository.PatientRepository
import com.example.cuidafamily.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Representa los diferentes estados de la interfaz de usuario para la ficha del paciente.
 */
sealed class PatientUiState {
    object Empty : PatientUiState()
    object Loading : PatientUiState()
    data class Loaded(val patient: Patient, val canEdit: Boolean) : PatientUiState()
    data class Error(val mensaje: String) : PatientUiState()
}

class PatientViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PatientUiState>(PatientUiState.Loading)
    val uiState: StateFlow<PatientUiState> = _uiState.asStateFlow()

    private var currentFamilyGroupId: String? = null
    private var currentUserRole: Role? = null
    private var loadPatientJob: Job? = null

    /**
     * Inicializa la carga del paciente con sincronización en tiempo real.
     */
    fun loadPatient(familyGroupId: String, userRole: Role) {
        currentFamilyGroupId = familyGroupId
        currentUserRole = userRole
        
        loadPatientJob?.cancel()
        loadPatientJob = viewModelScope.launch {
            _uiState.value = PatientUiState.Loading
            
            PatientRepository.obtenerPatientRealTime(familyGroupId).collect { patient ->
                if (patient != null) {
                    _uiState.value = PatientUiState.Loaded(
                        patient = patient,
                        canEdit = checkCanEdit(userRole)
                    )
                } else {
                    _uiState.value = PatientUiState.Empty
                }
            }
        }
    }

    /**
     * Crea o edita la información completa del paciente.
     */
    fun registrarOEditarPatient(
        nombre: String,
        fechaNacimiento: String,
        dni: String,
        grupoSanguineo: String,
        pesoActual: Double,
        historiaClinica: String = "",
        seguroSalud: String = "",
        medicoTratante: String = "",
        contactoEmergenciaNombre: String = "",
        contactoEmergenciaTelefono: String = "",
        contactoEmergenciaRelacion: String = "",
        enfermedadesCronicas: String = "",
        medicamentosActuales: String = "",
        alergiasIniciales: List<Allergy> = emptyList(),
        informacionAdicional: String? = null,
        fotoUrl: String? = null
    ) {
        val groupId = currentFamilyGroupId ?: return
        if (!checkCanEdit(currentUserRole)) return

        viewModelScope.launch {
            _uiState.value = PatientUiState.Loading
            try {
                // Obtenemos el paciente actual para conservar el ID si existe
                val existingPatientResult = PatientRepository.obtenerPatientDelGrupo(groupId)
                val patientId = if (existingPatientResult is Result.Success) {
                    existingPatientResult.data?.id ?: UUID.randomUUID().toString()
                } else {
                    UUID.randomUUID().toString()
                }

                val newPatient = Patient(
                    id = patientId,
                    nombre = nombre,
                    fechaNacimiento = fechaNacimiento,
                    dni = dni,
                    grupoSanguineo = grupoSanguineo,
                    pesoActual = pesoActual,
                    historiaClinica = historiaClinica,
                    seguroSalud = seguroSalud,
                    medicoTratante = medicoTratante,
                    contactoEmergenciaNombre = contactoEmergenciaNombre,
                    contactoEmergenciaTelefono = contactoEmergenciaTelefono,
                    contactoEmergenciaRelacion = contactoEmergenciaRelacion,
                    enfermedadesCronicas = enfermedadesCronicas,
                    medicamentosActuales = medicamentosActuales,
                    informacionAdicional = informacionAdicional,
                    fotoUrl = fotoUrl,
                    familyGroupId = groupId
                )

                val saveResult = PatientRepository.crearOEditarPatient(newPatient)
                if (saveResult is Result.Success) {
                    // Si hay alergias iniciales (caso ALTA), las guardamos una a una
                    alergiasIniciales.forEach { allergy ->
                        PatientRepository.agregarAlergia(groupId, patientId, allergy)
                    }
                } else if (saveResult is Result.Error) {
                    _uiState.value = PatientUiState.Error(saveResult.exception.message ?: "Error al guardar")
                }
            } catch (e: Exception) {
                _uiState.value = PatientUiState.Error(e.message ?: "Error inesperado al guardar el paciente")
            }
        }
    }

    /**
     * Agrega una alergia al perfil del paciente.
     */
    fun agregarAlergia(allergy: Allergy) {
        val state = _uiState.value
        val groupId = currentFamilyGroupId ?: return
        if (state is PatientUiState.Loaded && state.canEdit) {
            viewModelScope.launch {
                val result = PatientRepository.agregarAlergia(groupId, state.patient.id, allergy)
                if (result is Result.Error) {
                    _uiState.value = PatientUiState.Error(result.exception.message ?: "Error al guardar alergia")
                }
            }
        }
    }

    /**
     * Elimina una alergia del perfil del paciente.
     */
    fun eliminarAlergia(allergyId: String) {
        val state = _uiState.value
        val groupId = currentFamilyGroupId ?: return
        if (state is PatientUiState.Loaded && state.canEdit) {
            viewModelScope.launch {
                val result = PatientRepository.eliminarAlergia(groupId, state.patient.id, allergyId)
                if (result is Result.Error) {
                    _uiState.value = PatientUiState.Error(result.exception.message ?: "Error al eliminar alergia")
                }
            }
        }
    }

    /**
     * Registra un nuevo peso y actualiza la ficha.
     */
    fun actualizarPeso(fecha: String, valorKg: Double) {
        val state = _uiState.value
        val groupId = currentFamilyGroupId ?: return
        if (state is PatientUiState.Loaded) {
            val role = currentUserRole
            val canUpdateMedical = role == Role.ADMINISTRADOR_FAMILIAR || 
                                  role == Role.COLABORADOR || 
                                  role == Role.CUIDADOR_EXTERNO
            
            if (canUpdateMedical) {
                viewModelScope.launch {
                    val result = PatientRepository.agregarRegistroPeso(groupId, state.patient.id, WeightRecord(fecha, valorKg))
                    if (result is Result.Error) {
                        _uiState.value = PatientUiState.Error(result.exception.message ?: "Error al registrar peso")
                    }
                }
            }
        }
    }

    /**
     * Actualiza campos específicos del paciente.
     */
    fun actualizarCampos(cambios: Map<String, Any>) {
        val state = _uiState.value
        val groupId = currentFamilyGroupId ?: return
        if (state is PatientUiState.Loaded && state.canEdit) {
            viewModelScope.launch {
                val result = PatientRepository.actualizarCamposPatient(groupId, state.patient.id, cambios)
                if (result is Result.Error) {
                    _uiState.value = PatientUiState.Error(result.exception.message ?: "Error al actualizar")
                }
            }
        }
    }

    private fun checkCanEdit(role: Role?): Boolean {
        return role == Role.ADMINISTRADOR_FAMILIAR || role == Role.COLABORADOR
    }
}

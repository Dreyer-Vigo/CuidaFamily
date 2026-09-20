package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.Allergy
import com.example.cuidafamily.model.Patient
import com.example.cuidafamily.model.WeightRecord
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repositorio médico para la gestión de Pacientes, Alergias e Historial de Peso en Firestore.
 * Estructura: familyGroups/{groupId}/patients/{patientId}
 */
object PatientRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    /**
     * Obtiene el paciente asignado al grupo familiar con actualizaciones en tiempo real.
     */
    fun obtenerPatientRealTime(familyGroupId: String): Flow<Patient?> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            Log.e("PatientRepository", "obtenerPatientRealTime: Usuario no autenticado")
            trySend(null)
            close()
            return@callbackFlow
        }

        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            Log.e("PatientRepository", "obtenerPatientRealTime: ID de grupo inválido ($familyGroupId)")
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups").document(familyGroupId)
            .collection("patients")
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("obtenerPatientRealTime", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val patient = try {
                    snapshot?.documents?.firstOrNull()?.toObject(Patient::class.java)
                } catch (e: Exception) {
                    Log.e("PatientRepository", "Error al deserializar Patient en tiempo real", e)
                    null
                }
                trySend(patient)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Obtiene el paciente una única vez.
     */
    suspend fun obtenerPatientDelGrupo(familyGroupId: String): Result<Patient?> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))
        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") return@withContext Result.Error(Exception("ID de grupo inválido."))

        try {
            val query = db.collection("familyGroups").document(familyGroupId)
                .collection("patients")
                .limit(1)
                .get()
                .await()
            
            val patient = try {
                query.documents.firstOrNull()?.toObject(Patient::class.java)
            } catch (e: Exception) {
                Log.e("PatientRepository", "Error deserializing Patient for group: $familyGroupId", e)
                null
            }
            Result.Success(patient)
        } catch (e: Exception) {
            handleFirebaseException("obtenerPatientDelGrupo", e)
            Result.Error(Exception("No se pudo recuperar la información del paciente."))
        }
    }

    /**
     * Guarda o actualiza la ficha del paciente dentro de su grupo familiar.
     */
    suspend fun crearOEditarPatient(patient: Patient): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))
        
        val groupId = patient.familyGroupId
        if (groupId.isBlank() || groupId == "MOCK_GROUP_ID") {
            return@withContext Result.Error(Exception("No se puede guardar: ID de grupo inválido."))
        }

        Log.d("PatientRepository", "Guardando patient en grupo: $groupId")

        try {
            db.collection("familyGroups").document(groupId)
                .collection("patients").document(patient.id)
                .set(patient)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("crearOEditarPatient", e)
            Result.Error(Exception("No se pudo guardar la ficha del paciente. Verifique sus permisos."))
        }
    }

    /**
     * Elimina el paciente y sus datos asociados.
     */
    suspend fun eliminarPatient(familyGroupId: String, patientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))
        
        try {
            db.collection("familyGroups").document(familyGroupId)
                .collection("patients").document(patientId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("eliminarPatient", e)
            Result.Error(Exception("No se pudo eliminar el registro."))
        }
    }

    /**
     * Agrega o actualiza una alergia en la subcolección del paciente.
     */
    suspend fun agregarAlergia(familyGroupId: String, patientId: String, allergy: Allergy): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups").document(familyGroupId)
                .collection("patients").document(patientId)
                .collection("allergies").document(allergy.id)
                .set(allergy)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("agregarAlergia", e)
            Result.Error(Exception("Error al guardar la alergia."))
        }
    }

    /**
     * Elimina una alergia específica.
     */
    suspend fun eliminarAlergia(familyGroupId: String, patientId: String, allergyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups").document(familyGroupId)
                .collection("patients").document(patientId)
                .collection("allergies").document(allergyId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("eliminarAlergia", e)
            Result.Error(Exception("No se pudo eliminar la alergia."))
        }
    }

    /**
     * Registra un nuevo peso y actualiza el campo pesoActual en el paciente usando una transacción.
     */
    suspend fun agregarRegistroPeso(familyGroupId: String, patientId: String, weightRecord: WeightRecord): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val patientRef = db.collection("familyGroups").document(familyGroupId)
                .collection("patients").document(patientId)
            
            db.runTransaction { transaction ->
                val newWeightRef = patientRef.collection("weightHistory").document()
                transaction.set(newWeightRef, weightRecord)
                transaction.update(patientRef, "pesoActual", weightRecord.valorKg)
            }.await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("agregarRegistroPeso", e)
            Result.Error(Exception("No se pudo registrar el peso."))
        }
    }

    /**
     * Recupera el historial cronológico de peso desde Firestore.
     */
    suspend fun obtenerHistorialPeso(familyGroupId: String, patientId: String): Result<List<WeightRecord>> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val query = db.collection("familyGroups").document(familyGroupId)
                .collection("patients").document(patientId)
                .collection("weightHistory")
                .orderBy("fecha")
                .get()
                .await()
            
            val list = try {
                query.toObjects(WeightRecord::class.java)
            } catch (e: Exception) {
                Log.e("PatientRepository", "Error deserializing WeightRecord list for patient: $patientId", e)
                emptyList()
            }
            Result.Success(list)
        } catch (e: Exception) {
            handleFirebaseException("obtenerHistorialPeso", e)
            Result.Error(Exception("No se pudo recuperar el historial de peso."))
        }
    }

    /**
     * Obtiene la lista de alergias del paciente desde Firestore.
     */
    suspend fun obtenerAlergiasDelPatient(familyGroupId: String, patientId: String): Result<List<Allergy>> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val query = db.collection("familyGroups").document(familyGroupId)
                .collection("patients").document(patientId)
                .collection("allergies")
                .get()
                .await()
            
            val list = try {
                query.toObjects(Allergy::class.java)
            } catch (e: Exception) {
                Log.e("PatientRepository", "Error deserializing Allergy list for patient: $patientId", e)
                emptyList()
            }
            Result.Success(list)
        } catch (e: Exception) {
            handleFirebaseException("obtenerAlergiasDelPatient", e)
            Result.Error(Exception("No se pudieron cargar las alergias."))
        }
    }

    /**
     * Actualiza campos específicos del paciente.
     */
    suspend fun actualizarCamposPatient(familyGroupId: String, patientId: String, cambios: Map<String, Any>): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups").document(familyGroupId)
                .collection("patients").document(patientId)
                .update(cambios)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("actualizarCamposPatient", e)
            Result.Error(Exception("Error al actualizar la ficha."))
        }
    }

    private fun handleFirebaseException(method: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e("PatientRepository", "Firestore Error [$method]: Code=${e.code} - ${e.message}")
        } else if (e is FirebaseException) {
            Log.e("PatientRepository", "Firebase Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        } else {
            Log.e("PatientRepository", "General Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}

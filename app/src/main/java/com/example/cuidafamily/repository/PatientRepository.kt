package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.Allergy
import com.example.cuidafamily.model.Patient
import com.example.cuidafamily.model.WeightRecord
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object PatientRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

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
                
                val doc = snapshot?.documents?.firstOrNull()
                if (doc == null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val basicPatient = parseBasicPatient(doc)

                doc.reference.collection("datosSensibles").document("info")
                    .get()
                    .addOnSuccessListener { sensiblesDoc ->
                        val fullPatient = mergeSensibles(basicPatient, sensiblesDoc)
                        trySend(fullPatient)
                    }
                    .addOnFailureListener {
                        trySend(basicPatient)
                    }
            }
        
        awaitClose { listener.remove() }
    }

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
            
            val doc = query.documents.firstOrNull() ?: return@withContext Result.Success(null)
            val basicPatient = parseBasicPatient(doc)

            val fullPatient = try {
                val sensiblesDoc = doc.reference.collection("datosSensibles").document("info").get().await()
                mergeSensibles(basicPatient, sensiblesDoc)
            } catch (e: Exception) {
                basicPatient
            }

            Result.Success(fullPatient)
        } catch (e: Exception) {
            handleFirebaseException("obtenerPatientDelGrupo", e)
            Result.Error(Exception("No se pudo recuperar la información del paciente."))
        }
    }

    suspend fun crearOEditarPatient(patient: Patient): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))
        
        val groupId = patient.familyGroupId
        if (groupId.isBlank() || groupId == "MOCK_GROUP_ID") {
            return@withContext Result.Error(Exception("No se puede guardar: ID de grupo inválido."))
        }

        Log.d("PatientRepository", "Guardando patient en grupo: $groupId")

        try {
            val patientRef = db.collection("familyGroups").document(groupId)
                .collection("patients").document(patient.id)

            val basicMap = mapOf(
                "id" to patient.id,
                "nombre" to patient.nombre,
                "fechaNacimiento" to patient.fechaNacimiento,
                "fotoUrl" to patient.fotoUrl,
                "grupoSanguineo" to patient.grupoSanguineo,
                "pesoActual" to patient.pesoActual,
                "familyGroupId" to groupId
            )

            val sensiblesMap = mapOf(
                "dni" to patient.dni,
                "historiaClinica" to patient.historiaClinica,
                "seguroSalud" to patient.seguroSalud,
                "medicoTratante" to patient.medicoTratante,
                "contactoEmergenciaNombre" to patient.contactoEmergenciaNombre,
                "contactoEmergenciaTelefono" to patient.contactoEmergenciaTelefono,
                "contactoEmergenciaRelacion" to patient.contactoEmergenciaRelacion,
                "enfermedadesCronicas" to patient.enfermedadesCronicas,
                "medicamentosActuales" to patient.medicamentosActuales,
                "informacionAdicional" to patient.informacionAdicional
            )

            val batch = db.batch()
            batch.set(patientRef, basicMap, SetOptions.merge())
            batch.set(patientRef.collection("datosSensibles").document("info"), sensiblesMap, SetOptions.merge())
            batch.commit().await()

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("crearOEditarPatient", e)
            Result.Error(Exception("No se pudo guardar la ficha del paciente. Verifique sus permisos."))
        }
    }

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

    suspend fun actualizarCamposPatient(familyGroupId: String, patientId: String, cambios: Map<String, Any>): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val basicKeys = setOf("nombre", "fechaNacimiento", "grupoSanguineo", "pesoActual", "fotoUrl", "familyGroupId")
            val basicMap = cambios.filterKeys { it in basicKeys }
            val sensiblesMap = cambios.filterKeys { it !in basicKeys }

            val patientRef = db.collection("familyGroups").document(familyGroupId)
                .collection("patients").document(patientId)

            val batch = db.batch()
            if (basicMap.isNotEmpty()) {
                batch.update(patientRef, basicMap)
            }
            if (sensiblesMap.isNotEmpty()) {
                val sensiblesRef = patientRef.collection("datosSensibles").document("info")
                batch.set(sensiblesRef, sensiblesMap, SetOptions.merge())
            }
            batch.commit().await()

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("actualizarCamposPatient", e)
            Result.Error(Exception("Error al actualizar la ficha."))
        }
    }

    private fun parseBasicPatient(doc: DocumentSnapshot): Patient {
        return Patient(
            id = doc.id,
            nombre = doc.getString("nombre") ?: "",
            fechaNacimiento = doc.getString("fechaNacimiento") ?: "",
            fotoUrl = doc.getString("fotoUrl"),
            grupoSanguineo = doc.getString("grupoSanguineo") ?: "O+",
            pesoActual = doc.getDouble("pesoActual") ?: 0.0,
            familyGroupId = doc.getString("familyGroupId") ?: ""
        )
    }

    private fun mergeSensibles(basic: Patient, sensiblesDoc: DocumentSnapshot?): Patient {
        if (sensiblesDoc == null || !sensiblesDoc.exists()) return basic
        return basic.copy(
            dni = sensiblesDoc.getString("dni") ?: "",
            historiaClinica = sensiblesDoc.getString("historiaClinica") ?: "",
            seguroSalud = sensiblesDoc.getString("seguroSalud") ?: "",
            medicoTratante = sensiblesDoc.getString("medicoTratante") ?: "",
            contactoEmergenciaNombre = sensiblesDoc.getString("contactoEmergenciaNombre") ?: "",
            contactoEmergenciaTelefono = sensiblesDoc.getString("contactoEmergenciaTelefono") ?: "",
            contactoEmergenciaRelacion = sensiblesDoc.getString("contactoEmergenciaRelacion") ?: "",
            enfermedadesCronicas = sensiblesDoc.getString("enfermedadesCronicas") ?: "",
            medicamentosActuales = sensiblesDoc.getString("medicamentosActuales") ?: "",
            informacionAdicional = sensiblesDoc.getString("informacionAdicional")
        )
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

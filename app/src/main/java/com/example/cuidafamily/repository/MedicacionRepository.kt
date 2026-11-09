package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.EstadoToma
import com.example.cuidafamily.model.Medicamento
import com.example.cuidafamily.model.TomaMedicacion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// TODO: migrar a FCM + Cloud Functions para push real

object MedicacionRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    fun escucharMedicamentos(familyGroupId: String): Flow<List<Medicamento>> = callbackFlow {
        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("medicamentos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Medicacion", "Error listener: ${error.code} ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val horariosRaw = doc.get("horarios") as? List<*>
                        val horariosList = horariosRaw?.mapNotNull { it?.toString()?.trim() }?.map { normalizarHora(it) } ?: emptyList()

                        val diasRaw = doc.get("diasSemana") as? List<*>
                        val diasList = diasRaw?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()

                        Medicamento(
                            id = doc.id,
                            familyGroupId = doc.getString("familyGroupId") ?: familyGroupId,
                            nombre = doc.getString("nombre") ?: "",
                            dosis = doc.getString("dosis") ?: "",
                            horarios = horariosList,
                            diasSemana = diasList,
                            fechaInicio = doc.getString("fechaInicio") ?: "",
                            fechaFin = doc.getString("fechaFin")?.takeIf { it.isNotBlank() },
                            indicaciones = doc.getString("indicaciones") ?: "",
                            activo = doc.getBoolean("activo") ?: true,
                            creadoPorUserId = doc.getString("creadoPorUserId") ?: "",
                            creadoPorNombre = doc.getString("creadoPorNombre") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("Medicacion", "No se pudo leer el medicamento ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()

                trySend(lista.sortedBy { it.nombre })
            }

        awaitClose { listener.remove() }
    }

    private fun normalizarHora(hora: String): String {
        return try {
            val parts = hora.split(":")
            if (parts.size == 2) {
                val h = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                "$h:$m"
            } else hora
        } catch (e: Exception) {
            hora
        }
    }

    fun escucharTomas(familyGroupId: String, desdeFecha: String, hastaFecha: String): Flow<List<TomaMedicacion>> = callbackFlow {
        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("tomasMedicacion")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Medicacion", "Error listener tomas: ${error.code} ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val estadoStr = doc.getString("estado") ?: "CONFIRMADA"
                        val estado = try { EstadoToma.valueOf(estadoStr) } catch (e: Exception) { EstadoToma.CONFIRMADA }
                        TomaMedicacion(
                            id = doc.id,
                            familyGroupId = doc.getString("familyGroupId") ?: familyGroupId,
                            medicamentoId = doc.getString("medicamentoId") ?: "",
                            medicamentoNombre = doc.getString("medicamentoNombre") ?: "",
                            dosis = doc.getString("dosis") ?: "",
                            fecha = doc.getString("fecha") ?: "",
                            horaProgramada = doc.getString("horaProgramada") ?: "",
                            estado = estado,
                            confirmadoPorUserId = doc.getString("confirmadoPorUserId") ?: "",
                            confirmadoPorNombre = doc.getString("confirmadoPorNombre") ?: "",
                            confirmadoPorRol = doc.getString("confirmadoPorRol") ?: "",
                            horaConfirmacion = doc.getLong("horaConfirmacion") ?: 0L,
                            nota = doc.getString("nota") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("Medicacion", "No se pudo leer la toma ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()

                val filtradas = lista.filter { it.fecha in desdeFecha..hastaFecha }
                trySend(filtradas.sortedWith(compareBy({ it.fecha }, { it.horaProgramada })))
            }

        awaitClose { listener.remove() }
    }

    suspend fun crearOEditarMedicamento(familyGroupId: String, medicamento: Medicamento): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))
        if (familyGroupId.isBlank()) return@withContext Result.Error(Exception("Grupo familiar no válido."))

        try {
            val collectionRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("medicamentos")

            val docRef = if (medicamento.id.isNotBlank()) collectionRef.document(medicamento.id) else collectionRef.document()
            val finalMed = medicamento.copy(id = docRef.id, familyGroupId = familyGroupId)

            val esNuevo = medicamento.id.isBlank()
            docRef.set(finalMed, SetOptions.merge()).await()

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.MEDICACION,
                accion = if (esNuevo) com.example.cuidafamily.model.AccionAuditoria.CREAR else com.example.cuidafamily.model.AccionAuditoria.MODIFICAR,
                entidadId = finalMed.id,
                descripcion = if (esNuevo) "Creó el medicamento '${finalMed.nombre}'" else "Modificó el medicamento '${finalMed.nombre}'",
                detalleCambios = "Dosis: ${finalMed.dosis}, Horarios: ${finalMed.horarios.joinToString()}"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("Medicacion", "Error guardando medicamento: ${e.message}", e)
            Result.Error(Exception("No se pudo guardar el medicamento."))
        }
    }

    suspend fun desactivarMedicamento(familyGroupId: String, medicamentoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("medicamentos")
                .document(medicamentoId)
                .update("activo", false)
                .await()

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.MEDICACION,
                accion = com.example.cuidafamily.model.AccionAuditoria.ELIMINAR,
                entidadId = medicamentoId,
                descripcion = "Desactivó un medicamento",
                detalleCambios = "Medicamento desactivado de la lista activa"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("Medicacion", "Error desactivando medicamento: ${e.message}", e)
            Result.Error(Exception("No se pudo desactivar el medicamento."))
        }
    }

    suspend fun confirmarToma(familyGroupId: String, toma: TomaMedicacion): Result<String> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val docRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("tomasMedicacion")
                .document(toma.id)

            val confirmador = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val existente = snapshot.toObject(TomaMedicacion::class.java)
                    existente?.confirmadoPorNombre ?: "Otro usuario"
                } else {
                    transaction.set(docRef, toma)
                    null
                }
            }.await()

            if (confirmador != null) {
                Result.Success(confirmador)
            } else {
                Result.Success("")
            }
        } catch (e: Exception) {
            Log.e("Medicacion", "Error al confirmar toma: ${e.message}", e)
            Result.Error(Exception("No se pudo confirmar la toma."))
        }
    }

    suspend fun marcarOmitida(familyGroupId: String, toma: TomaMedicacion): Result<String> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val docRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("tomasMedicacion")
                .document(toma.id)

            val confirmador = db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val existente = snapshot.toObject(TomaMedicacion::class.java)
                    existente?.confirmadoPorNombre ?: "Otro usuario"
                } else {
                    transaction.set(docRef, toma)
                    null
                }
            }.await()

            if (confirmador != null) {
                Result.Success(confirmador)
            } else {
                Result.Success("")
            }
        } catch (e: Exception) {
            Log.e("Medicacion", "Error al marcar toma omitida: ${e.message}", e)
            Result.Error(Exception("No se pudo marcar la toma como omitida."))
        }
    }
}

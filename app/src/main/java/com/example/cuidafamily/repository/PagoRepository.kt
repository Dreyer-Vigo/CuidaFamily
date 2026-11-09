package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.Pago
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.TarifaTurno
import com.example.cuidafamily.model.TurnoCheckIn
import com.example.cuidafamily.util.Permisos
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object PagoRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    /**
     * Requerimiento 4: Crear un nuevo registro de pago.
     * Colección: "familyGroups/{groupId}/pagos/{id}"
     */
    suspend fun crearPago(
        familyGroupId: String,
        pago: Pago,
        userRole: Role?
    ): Result<Pago> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            val err = "Error al guardar: Sesión no válida."
            Log.e("Pagos", err)
            return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))
        }
        if (!Permisos.puedeVerFinanzas(userRole)) {
            val err = "Error al guardar: Permiso denegado para rol=$userRole."
            Log.e("Pagos", err)
            return@withContext Result.Error(Exception("No tienes permiso financiero."))
        }

        // Requerimiento 7: Log de inicio de guardado
        Log.d("Pagos", "Guardando pago: cuidador=${pago.cuidadorUserId}, monto=${pago.monto}, periodoFin=${pago.periodoFin}, grupo=$familyGroupId")

        try {
            // Requerimiento 1: Rechazar un pago cuyo cuidadorUserId no pertenezca a un miembro con rol CUIDADOR_EXTERNO
            val memberDoc = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("members")
                .document(pago.cuidadorUserId)
                .get()
                .await()

            val memberRoleStr = memberDoc.getString("role") ?: ""
            if (memberRoleStr != Role.CUIDADOR_EXTERNO.name) {
                val errorMsg = "Solo se puede pagar a cuidadores externos"
                Log.e("Pagos", "Error al guardar: $errorMsg")
                return@withContext Result.Error(Exception(errorMsg))
            }

            val colRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("pagos")

            val docRef = if (pago.id.isNotBlank()) colRef.document(pago.id) else colRef.document()

            val finalPago = pago.copy(
                id = docRef.id,
                familyGroupId = familyGroupId,
                registradoPorUserId = user.uid,
                registradoPorNombre = pago.registradoPorNombre.ifBlank { "Familiar" },
                fechaRegistro = if (pago.fechaRegistro > 0L) pago.fechaRegistro else System.currentTimeMillis()
            )

            docRef.set(finalPago).await()

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.PAGO,
                accion = com.example.cuidafamily.model.AccionAuditoria.CREAR,
                entidadId = docRef.id,
                descripcion = "Registró pago a ${finalPago.cuidadorNombre}: ${com.example.cuidafamily.util.formatearSoles(finalPago.monto)}",
                detalleCambios = "Periodo: ${finalPago.periodoInicio} al ${finalPago.periodoFin}, Método: ${finalPago.metodo.etiqueta}"
            )

            // Requerimiento 7: Log OK
            Log.d("Pagos", "Guardado OK: id=${docRef.id}")
            Result.Success(finalPago)
        } catch (e: Exception) {
            Log.e("Pagos", "Error al guardar: ${e.message}", e)
            handleFirebaseException("crearPago", e)
            Result.Error(Exception("No se pudo registrar el pago: ${e.message}"))
        }
    }

    /**
     * Requerimiento 4: Editar pago existente.
     * Editar solo si registradoPorUserId == uid actual.
     * Al editar NUNCA cambies registradoPorUserId, registradoPorNombre ni fechaRegistro (update parcial).
     */
    suspend fun editarPago(
        familyGroupId: String,
        pago: Pago,
        userRole: Role?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            Log.e("PagoRepository", "Error al editar pago: Sesión no válida.")
            return@withContext Result.Error(Exception("Sesión no válida."))
        }
        if (!Permisos.puedeVerFinanzas(userRole)) {
            Log.e("PagoRepository", "Error al editar pago: Permiso denegado para rol=$userRole.")
            return@withContext Result.Error(Exception("No tienes permiso financiero."))
        }

        try {
            // Requerimiento 1: Rechazar un pago cuyo cuidadorUserId no pertenezca a un miembro con rol CUIDADOR_EXTERNO
            val memberDoc = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("members")
                .document(pago.cuidadorUserId)
                .get()
                .await()

            val memberRoleStr = memberDoc.getString("role") ?: ""
            if (memberRoleStr != Role.CUIDADOR_EXTERNO.name) {
                val errorMsg = "Solo se puede pagar a cuidadores externos"
                Log.e("PagoRepository", errorMsg)
                return@withContext Result.Error(Exception(errorMsg))
            }

            val docRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("pagos")
                .document(pago.id)

            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                Log.e("PagoRepository", "Error al editar pago: El registro no existe en Firestore.")
                return@withContext Result.Error(Exception("El registro de pago no existe."))
            }

            val autorRegistradoUid = snapshot.getString("registradoPorUserId") ?: ""
            if (autorRegistradoUid != user.uid) {
                val errorMsg = "Solo quien registró este pago puede modificarlo"
                Log.e("PagoRepository", errorMsg)
                return@withContext Result.Error(Exception(errorMsg))
            }

            // Update parcial: Modificar solo los campos editables sin alterar registradoPorUserId, registradoPorNombre ni fechaRegistro
            val updates = mapOf(
                "cuidadorUserId" to pago.cuidadorUserId,
                "cuidadorNombre" to pago.cuidadorNombre,
                "monto" to pago.monto,
                "fecha" to pago.fecha,
                "periodoInicio" to pago.periodoInicio,
                "periodoFin" to pago.periodoFin,
                "metodo" to pago.metodo.name,
                "referencia" to pago.referencia.trim(),
                "nota" to pago.nota.trim()
            )

            docRef.update(updates).await()
            Log.d("PagoRepository", "Pago ID=${pago.id} actualizado mediante update() parcial por UID=${user.uid}")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.PAGO,
                accion = com.example.cuidafamily.model.AccionAuditoria.MODIFICAR,
                entidadId = pago.id,
                descripcion = "Modificó pago a ${pago.cuidadorNombre}: ${com.example.cuidafamily.util.formatearSoles(pago.monto)}",
                detalleCambios = "Periodo: ${pago.periodoInicio} al ${pago.periodoFin}"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("editarPago", e)
            Result.Error(Exception("No se pudo actualizar el pago."))
        }
    }

    /**
     * Requerimiento 4: Eliminar pago existente.
     * Solo si registradoPorUserId == uid actual.
     */
    suspend fun eliminarPago(
        familyGroupId: String,
        pagoId: String,
        userRole: Role?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            Log.e("PagoRepository", "Error al eliminar pago: Sesión no válida.")
            return@withContext Result.Error(Exception("Sesión no válida."))
        }
        if (!Permisos.puedeVerFinanzas(userRole)) {
            Log.e("PagoRepository", "Error al eliminar pago: Permiso denegado para rol=$userRole.")
            return@withContext Result.Error(Exception("No tienes permiso financiero."))
        }

        try {
            val docRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("pagos")
                .document(pagoId)

            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                Log.e("PagoRepository", "Error al eliminar pago: El registro no existe en Firestore.")
                return@withContext Result.Error(Exception("El registro de pago no existe."))
            }

            val autorRegistradoUid = snapshot.getString("registradoPorUserId") ?: ""
            if (autorRegistradoUid != user.uid) {
                val errorMsg = "Solo quien registró este pago puede modificarlo"
                Log.e("PagoRepository", errorMsg)
                return@withContext Result.Error(Exception(errorMsg))
            }

            docRef.delete().await()
            Log.d("PagoRepository", "Pago ID=$pagoId eliminado exitosamente por UID=${user.uid}")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.PAGO,
                accion = com.example.cuidafamily.model.AccionAuditoria.ELIMINAR,
                entidadId = pagoId,
                descripcion = "Eliminó un registro de pago",
                detalleCambios = "Pago eliminado de Firestore"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("eliminarPago", e)
            Result.Error(Exception("No se pudo eliminar el pago."))
        }
    }

    /**
     * Requerimiento 4: Escuchar pagos en tiempo real (filtra y ordena en memoria, sin índices Firestore).
     */
    fun escucharPagos(
        familyGroupId: String,
        userRole: Role?
    ): Flow<List<Pago>> = callbackFlow {
        val user = auth.currentUser
        if (user == null || !Permisos.puedeVerFinanzas(userRole) || familyGroupId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("pagos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("escucharPagos", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val p = doc.toObject(Pago::class.java)
                        p?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("Pagos", "Error deserializando Pago ID=${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                // Requerimiento 7: Log de lectura
                Log.d("Pagos", "Recibidos ${lista.size} pagos del grupo")

                // Filtrar y ordenar en memoria por fechaRegistro descendente
                val ordenada = lista.sortedByDescending { it.fechaRegistro }
                trySend(ordenada)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Requerimiento 1: Guardar Tarifa en subcolección "familyGroups/{groupId}/tarifas/{turnoId}"
     */
    suspend fun guardarTarifa(
        familyGroupId: String,
        turnoId: String,
        tarifaPorHora: Double,
        userRole: Role?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null || !Permisos.puedeVerFinanzas(userRole)) {
            return@withContext Result.Error(Exception("Sin permiso para gestionar tarifas."))
        }

        try {
            val docRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("tarifas")
                .document(turnoId)

            val tarifaData = mapOf(
                "turnoId" to turnoId,
                "tarifaPorHora" to tarifaPorHora
            )

            docRef.set(tarifaData, SetOptions.merge()).await()
            Log.d("PagoRepository", "Tarifa de turno $turnoId guardada: S/ $tarifaPorHora / h")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.TURNO,
                accion = com.example.cuidafamily.model.AccionAuditoria.MODIFICAR,
                entidadId = turnoId,
                descripcion = "Estableció tarifa por hora del turno: ${com.example.cuidafamily.util.formatearSoles(tarifaPorHora)}",
                detalleCambios = "Tarifa por hora: ${com.example.cuidafamily.util.formatearSoles(tarifaPorHora)}"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("guardarTarifa", e)
            Result.Error(Exception("No se pudo guardar la tarifa."))
        }
    }

    /**
     * Requerimiento 1: Escuchar todas las tarifas en "familyGroups/{groupId}/tarifas"
     */
    fun escucharTarifas(
        familyGroupId: String,
        userRole: Role?
    ): Flow<Map<String, Double>> = callbackFlow {
        val user = auth.currentUser
        if (user == null || !Permisos.puedeVerFinanzas(userRole) || familyGroupId.isBlank()) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("tarifas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("escucharTarifas", error)
                    trySend(emptyMap())
                    return@addSnapshotListener
                }

                val mapa = mutableMapOf<String, Double>()
                snapshot?.documents?.forEach { doc ->
                    val tId = doc.getString("turnoId") ?: doc.id
                    val tarifaVal = doc.getDouble("tarifaPorHora") ?: 0.0
                    mapa[tId] = tarifaVal
                }

                trySend(mapa)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Requerimiento 2: Marcar cumplimiento manual o no cumplimiento de una ocurrencia.
     * Crea o actualiza un documento en "familyGroups/{groupId}/turnoCheckIns".
     */
    suspend fun marcarCumplimientoOcurrencia(
        familyGroupId: String,
        turnoId: String,
        fechaIso: String,
        cuidadorUserId: String,
        manual: Boolean,
        noCumplido: Boolean,
        currentUserId: String,
        currentUserName: String,
        userRole: Role?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null || !Permisos.puedeVerFinanzas(userRole)) {
            return@withContext Result.Error(Exception("Sin permiso financiero para modificar turnos."))
        }

        try {
            val colRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnoCheckIns")

            // Buscar si ya existe un checkIn para este turno y fecha
            val query = colRef
                .whereEqualTo("turnoId", turnoId)
                .whereEqualTo("fecha", fechaIso)
                .get()
                .await()

            val docRef = if (!query.isEmpty) {
                query.documents.first().reference
            } else {
                colRef.document("${turnoId}_${fechaIso.replace("-", "")}")
            }

            val updates = mapOf(
                "id" to docRef.id,
                "turnoId" to turnoId,
                "cuidadorUserId" to cuidadorUserId,
                "fecha" to fechaIso,
                "manual" to manual,
                "noCumplido" to noCumplido,
                "registradoPorUserId" to currentUserId,
                "registradoPorNombre" to currentUserName
            )

            docRef.set(updates, SetOptions.merge()).await()
            Log.d("PagoRepository", "Cumplimiento de ocurrencia ($turnoId, $fechaIso) actualizado: manual=$manual, noCumplido=$noCumplido por $currentUserName")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("marcarCumplimientoOcurrencia", e)
            Result.Error(Exception("No se pudo cambiar el estado de la ocurrencia."))
        }
    }

    /**
     * Escuchar todos los CheckIns en "familyGroups/{groupId}/turnoCheckIns"
     */
    fun escucharCheckIns(familyGroupId: String): Flow<List<TurnoCheckIn>> = callbackFlow {
        val user = auth.currentUser
        if (user == null || familyGroupId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("turnoCheckIns")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("escucharCheckIns", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(TurnoCheckIn::class.java)
                    } catch (e: Exception) {
                        Log.e("PagoRepository", "Error deserializando TurnoCheckIn ID=${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(lista)
            }

        awaitClose { listener.remove() }
    }

    private fun handleFirebaseException(method: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e("PagoRepository", "Firestore Error [$method]: Code=${e.code} - ${e.message}")
        } else if (e is FirebaseException) {
            Log.e("PagoRepository", "Firebase Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        } else {
            Log.e("PagoRepository", "General Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}

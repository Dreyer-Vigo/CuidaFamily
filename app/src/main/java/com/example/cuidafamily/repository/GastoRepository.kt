package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.Gasto
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.util.Permisos
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

object GastoRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    fun obtenerGastos(familyGroupId: String, rol: Role?): Flow<List<Gasto>> = callbackFlow {
        val acceso = Permisos.puedeVerFinanzas(rol)
        Log.d("Gastos", "rol=$rol, acceso=$acceso")

        if (!acceso) {
            Log.e("Gastos", "Error: Intento de acceso sin permisos a obtenerGastos. rol=$rol, acceso=false")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val user = auth.currentUser
        if (user == null || familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("gastos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("obtenerGastos", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Gasto::class.java)
                    } catch (e: Exception) {
                        Log.e("Gastos", "Error deserializando Gasto ${doc.id}", e)
                        null
                    }
                }?.sortedByDescending { it.fecha } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun crearGasto(
        familyGroupId: String,
        gasto: Gasto,
        rol: Role?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val acceso = Permisos.puedeVerFinanzas(rol)
        Log.d("Gastos", "rol=$rol, acceso=$acceso")

        if (!acceso) {
            Log.e("Gastos", "Error: Intento de acceso sin permisos a crearGasto. rol=$rol, acceso=false")
            return@withContext Result.Error(Exception("Sin acceso"))
        }

        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))
        if (gasto.concepto.isBlank() || gasto.monto <= 0.0) {
            return@withContext Result.Error(Exception("Ingresa un concepto y monto válidos."))
        }

        val colRef = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("gastos")

        val docRef = if (gasto.id.isNotBlank()) colRef.document(gasto.id) else colRef.document()

        // Requerimiento 2: Al crear, registradoPorUserId siempre es el UID de la sesión activa
        val finalGasto = gasto.copy(
            id = docRef.id,
            familyGroupId = familyGroupId,
            registradoPorUserId = user.uid
        )

        Log.d("Gastos", "uid=${user.uid}, autor=${finalGasto.registradoPorUserId}, puedeModificar=true")

        try {
            docRef.set(finalGasto).await()
            Log.d("Gastos", "Gasto guardado exitosamente id=${docRef.id} por ${finalGasto.registradoPorNombre}")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.GASTO,
                accion = com.example.cuidafamily.model.AccionAuditoria.CREAR,
                entidadId = docRef.id,
                descripcion = "Registró gasto: '${finalGasto.concepto}' por ${com.example.cuidafamily.util.formatearSoles(finalGasto.monto)}",
                detalleCambios = "Categoría: ${finalGasto.categoria.ifBlank { "General" }}, Fecha: ${finalGasto.fecha}"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("Gastos", "Fallo al guardar gasto con comprobante: ${e.message}", e)
            val esErrorTamano = isFirestoreSizeError(e)
            if (esErrorTamano && (finalGasto.comprobantesBase64.isNotEmpty() || finalGasto.comprobanteBase64.isNotBlank())) {
                Log.w("Gastos", "Reintentando guardar gasto sin comprobantesBase64 por límite de tamaño...")
                try {
                    val gastoSinComprobante = finalGasto.copy(
                        comprobantesBase64 = emptyList(),
                        comprobanteBase64 = ""
                    )
                    docRef.set(gastoSinComprobante).await()
                    Log.d("Gastos", "Gasto ${docRef.id} guardado sin copia compartida")
                    Result.Error(Exception("Comprobante demasiado grande para compartir con el grupo; el gasto se guardó sin la copia adjunta."))
                } catch (e2: Exception) {
                    Log.e("Gastos", "Fallo en reintento de guardado sin comprobante: ${e2.message}", e2)
                    handleFirebaseException("crearGasto", e2)
                    Result.Error(Exception("No se pudo registrar el gasto."))
                }
            } else {
                handleFirebaseException("crearGasto", e)
                Result.Error(Exception("No se pudo registrar el gasto."))
            }
        }
    }

    suspend fun editarGasto(
        familyGroupId: String,
        gasto: Gasto,
        rol: Role?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val acceso = Permisos.puedeVerFinanzas(rol)
        if (!acceso) {
            Log.e("Gastos", "Error: Intento de acceso sin permisos a editarGasto. rol=$rol, acceso=false")
            return@withContext Result.Error(Exception("Sin acceso"))
        }

        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        val docRef = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("gastos")
            .document(gasto.id)

        try {
            // Requerimiento 2: Verificar primero que registradoPorUserId coincida con el UID actual
            val docSnapshot = docRef.get().await()
            val autorExistente = docSnapshot.getString("registradoPorUserId") ?: ""
            val puede = (autorExistente == user.uid)

            Log.d("Gastos", "uid=${user.uid}, autor=$autorExistente, puedeModificar=$puede")

            if (!puede) {
                Log.e("Gastos", "Intento de modificar un gasto ajeno: ${gasto.id}")
                return@withContext Result.Error(Exception("Solo quien registró este gasto puede modificarlo"))
            }

            // Requerimiento 2: NUNCA cambiar registradoPorUserId, registradoPorNombre ni fechaRegistro (update parcial)
            val updates = mapOf<String, Any?>(
                "concepto" to gasto.concepto,
                "monto" to gasto.monto,
                "categoria" to gasto.categoria,
                "fecha" to gasto.fecha,
                "comprobanteUrl" to gasto.comprobanteUrl,
                "comprobanteBase64" to gasto.comprobanteBase64,
                "comprobantesBase64" to gasto.comprobantesBase64
            )

            docRef.update(updates).await()
            Log.d("Gastos", "Gasto ${gasto.id} actualizado con éxito mediante update parcial")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.GASTO,
                accion = com.example.cuidafamily.model.AccionAuditoria.MODIFICAR,
                entidadId = gasto.id,
                descripcion = "Modificó gasto: '${gasto.concepto}' (${com.example.cuidafamily.util.formatearSoles(gasto.monto)})",
                detalleCambios = "Categoría: ${gasto.categoria}, Fecha: ${gasto.fecha}"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("Gastos", "Fallo al editar gasto: ${e.message}", e)
            val esErrorTamano = isFirestoreSizeError(e)
            if (esErrorTamano && (gasto.comprobantesBase64.isNotEmpty() || gasto.comprobanteBase64.isNotBlank())) {
                Log.w("Gastos", "Reintentando editar gasto sin comprobantesBase64 por límite de tamaño...")
                try {
                    val updatesSinBase64 = mapOf<String, Any?>(
                        "concepto" to gasto.concepto,
                        "monto" to gasto.monto,
                        "categoria" to gasto.categoria,
                        "fecha" to gasto.fecha,
                        "comprobanteUrl" to gasto.comprobanteUrl,
                        "comprobanteBase64" to "",
                        "comprobantesBase64" to emptyList<String>()
                    )
                    docRef.update(updatesSinBase64).await()
                    Result.Error(Exception("Comprobante demasiado grande para compartir con el grupo; el gasto se guardó sin la copia adjunta."))
                } catch (e2: Exception) {
                    Log.e("Gastos", "Fallo en reintento de edición sin comprobante: ${e2.message}", e2)
                    handleFirebaseException("editarGasto", e2)
                    Result.Error(Exception("No se pudo actualizar el gasto."))
                }
            } else {
                handleFirebaseException("editarGasto", e)
                Result.Error(Exception(e.message ?: "No se pudo actualizar el gasto."))
            }
        }
    }

    suspend fun eliminarGasto(
        familyGroupId: String,
        gasto: Gasto,
        currentUserId: String,
        rol: Role?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val acceso = Permisos.puedeVerFinanzas(rol)
        if (!acceso) {
            Log.e("Gastos", "Error: Intento de acceso sin permisos a eliminarGasto. rol=$rol, acceso=false")
            return@withContext Result.Error(Exception("Sin acceso"))
        }

        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        val docRef = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("gastos")
            .document(gasto.id)

        try {
            // Requerimiento 2: Verificar en servidor que registradoPorUserId coincida con el UID del usuario actual
            val docSnapshot = docRef.get().await()
            val autorExistente = docSnapshot.getString("registradoPorUserId") ?: ""
            val puede = (autorExistente == user.uid)

            Log.d("Gastos", "uid=${user.uid}, autor=$autorExistente, puedeModificar=$puede")

            if (!puede) {
                Log.e("Gastos", "Intento de modificar un gasto ajeno: ${gasto.id}")
                return@withContext Result.Error(Exception("Solo quien registró este gasto puede modificarlo"))
            }

            docRef.delete().await()
            Log.d("Gastos", "Gasto ${gasto.id} eliminado correctamente")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.GASTO,
                accion = com.example.cuidafamily.model.AccionAuditoria.ELIMINAR,
                entidadId = gasto.id,
                descripcion = "Eliminó gasto: '${gasto.concepto}' (${com.example.cuidafamily.util.formatearSoles(gasto.monto)})",
                detalleCambios = "Categoría: ${gasto.categoria}"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("eliminarGasto", e)
            Result.Error(Exception(e.message ?: "No se pudo eliminar el gasto."))
        }
    }

    // Requerimiento 3: Reclamar gastos antiguos sin propietario (registradoPorUserId "")
    suspend fun reclamarGastoComoMio(
        familyGroupId: String,
        gastoId: String,
        currentUserId: String,
        currentUserName: String,
        rol: Role?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val acceso = Permisos.puedeVerFinanzas(rol)
        if (!acceso) return@withContext Result.Error(Exception("Sin acceso"))

        if (rol != Role.ADMINISTRADOR_FAMILIAR) {
            return@withContext Result.Error(Exception("Solo el Administrador Familiar puede reclamar gastos antiguos sin propietario."))
        }

        val user = auth.currentUser ?: return@withContext Result.Error(Exception("Sesión no válida."))

        val docRef = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("gastos")
            .document(gastoId)

        try {
            val docSnapshot = docRef.get().await()
            val autorExistente = docSnapshot.getString("registradoPorUserId") ?: ""

            if (autorExistente.isNotBlank()) {
                return@withContext Result.Error(Exception("Este gasto ya tiene un propietario asignado."))
            }

            val updates = mapOf<String, Any>(
                "registradoPorUserId" to user.uid,
                "registradoPorNombre" to currentUserName
            )

            docRef.update(updates).await()
            Log.d("Gastos", "Gasto $gastoId reclamado por ${user.uid}")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("reclamarGastoComoMio", e)
            Result.Error(Exception("No se pudo reclamar el gasto."))
        }
    }

    private fun isFirestoreSizeError(e: Exception): Boolean {
        val msg = e.message ?: ""
        return msg.contains("1MB", ignoreCase = true) ||
                msg.contains("1,048,576", ignoreCase = true) ||
                msg.contains("size", ignoreCase = true) ||
                msg.contains("large", ignoreCase = true) ||
                msg.contains("bytes", ignoreCase = true) ||
                (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.INVALID_ARGUMENT)
    }

    private fun handleFirebaseException(method: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e("Gastos", "Firestore Error [$method]: Code=${e.code} - ${e.message}")
        } else if (e is FirebaseException) {
            Log.e("Gastos", "Firebase Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        } else {
            Log.e("Gastos", "General Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}

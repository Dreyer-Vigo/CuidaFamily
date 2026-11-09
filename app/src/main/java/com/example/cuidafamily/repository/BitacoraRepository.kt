package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.BitacoraEntrada
import com.example.cuidafamily.model.EstadoAnimo
import com.example.cuidafamily.model.Role
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BitacoraRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    suspend fun crearEntrada(familyGroupId: String, entrada: BitacoraEntrada): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            Log.e("BitacoraRepository", "crearEntrada: Usuario no autenticado")
            return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))
        }
        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            return@withContext Result.Error(Exception("ID de grupo no válido."))
        }

        try {
            val collectionRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("bitacora")

            val docRef = if (entrada.id.isNotBlank()) collectionRef.document(entrada.id) else collectionRef.document()
            val fechaActual = if (entrada.fecha.isNotBlank()) entrada.fecha else SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val finalEntrada = entrada.copy(
                id = docRef.id,
                familyGroupId = familyGroupId,
                fecha = fechaActual
            )

            docRef.set(finalEntrada.toMap()).await()
            Log.d("BitacoraRepository", "Entrada guardada en bitácora con ID: ${docRef.id}")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("crearEntrada", e)
            Result.Error(Exception("No se pudo publicar en la bitácora. Verifique sus permisos."))
        }
    }

    fun obtenerEntradas(familyGroupId: String): Flow<List<BitacoraEntrada>> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            Log.e("BitacoraRepository", "obtenerEntradas: Usuario no autenticado")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            Log.e("BitacoraRepository", "obtenerEntradas: ID de grupo no válido ($familyGroupId)")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("bitacora")
            .whereEqualTo("familyGroupId", familyGroupId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("obtenerEntradas", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toBitacoraEntrada()
                } ?: emptyList()

                Log.d("BitacoraRepository", "Escuchando bitácora del grupo: $familyGroupId, entradas recibidas: ${list.size}")
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun eliminarEntrada(familyGroupId: String, entradaId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            return@withContext Result.Error(Exception("Sesión no válida."))
        }
        if (familyGroupId.isBlank() || entradaId.isBlank()) {
            return@withContext Result.Error(Exception("Parámetros de publicación no válidos."))
        }

        try {
            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("bitacora")
                .document(entradaId)
                .delete()
                .await()

            Log.d("BitacoraRepository", "Entrada $entradaId eliminada correctamente")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.BITACORA,
                accion = com.example.cuidafamily.model.AccionAuditoria.ELIMINAR,
                entidadId = entradaId,
                descripcion = "Eliminó una publicación de la bitácora",
                detalleCambios = "Entrada eliminada de Firestore"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("eliminarEntrada", e)
            Result.Error(Exception("No se pudo eliminar la publicación."))
        }
    }

    suspend fun actualizarUltimaVistaBitacora(familyGroupId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null || familyGroupId.isBlank() || userId.isBlank()) {
            return@withContext Result.Error(Exception("Parámetros inválidos."))
        }

        try {
            val memberRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("members")
                .document(userId)

            val nowMs = System.currentTimeMillis()

            try {
                memberRef.update("bitacoraUltimaVista", nowMs).await()
            } catch (e: Exception) {
                memberRef.set(mapOf("bitacoraUltimaVista" to nowMs), SetOptions.merge()).await()
            }

            Log.d("BitacoraRepository", "bitacoraUltimaVista actualizada a $nowMs para $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("actualizarUltimaVistaBitacora", e)
            Result.Error(Exception("No se pudo actualizar la marca de lectura."))
        }
    }

    suspend fun obtenerUltimaVistaBitacora(familyGroupId: String, userId: String): Long = withContext(Dispatchers.IO) {
        if (familyGroupId.isBlank() || userId.isBlank()) return@withContext 0L

        try {
            val memberDoc = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("members")
                .document(userId)
                .get()
                .await()

            memberDoc.getLong("bitacoraUltimaVista") ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun DocumentSnapshot.toBitacoraEntrada(): BitacoraEntrada? {
        return try {
            val familyGroupId = getString("familyGroupId") ?: ""
            val autorUserId = getString("autorUserId") ?: ""
            val autorNombre = getString("autorNombre") ?: ""
            val autorRolStr = getString("autorRol") ?: ""
            val autorRol = Role.fromString(autorRolStr) ?: Role.COLABORADOR
            val fecha = getString("fecha") ?: ""
            val estadoAnimoStr = getString("estadoAnimo") ?: ""
            val estadoAnimo = EstadoAnimo.fromString(estadoAnimoStr)
            val nota = getString("nota") ?: ""
            val fotoUrl = getString("fotoUrl") ?: ""

            BitacoraEntrada(
                id = id,
                familyGroupId = familyGroupId,
                autorUserId = autorUserId,
                autorNombre = autorNombre,
                autorRol = autorRol,
                fecha = fecha,
                estadoAnimo = estadoAnimo,
                nota = nota,
                fotoUrl = fotoUrl
            )
        } catch (e: Exception) {
            Log.e("BitacoraRepository", "Error deserializando BitacoraEntrada", e)
            null
        }
    }

    private fun BitacoraEntrada.toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "familyGroupId" to familyGroupId,
            "autorUserId" to autorUserId,
            "autorNombre" to autorNombre,
            "autorRol" to autorRol.name,
            "fecha" to fecha,
            "estadoAnimo" to estadoAnimo.name,
            "nota" to nota,
            "fotoUrl" to fotoUrl
        )
    }

    private fun handleFirebaseException(method: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e("BitacoraRepository", "Firestore Error [$method]: Code=${e.code} - ${e.message}")
        } else if (e is FirebaseException) {
            Log.e("BitacoraRepository", "Firebase Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        } else {
            Log.e("BitacoraRepository", "General Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}

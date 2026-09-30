package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.SosAlert
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: migrar a FCM + Cloud Functions para notificación push real entre dispositivos cerrados.

/**
 * Repositorio para la gestión de alertas de pánico SOS en Firestore.
 * Estructura en Firestore: familyGroups/{groupId}/sosAlerts/{alertId}
 */
object SosRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    /**
     * Dispara una nueva alerta SOS en el grupo familiar.
     */
    suspend fun dispararAlerta(
        familyGroupId: String,
        userId: String,
        nombre: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))
        }
        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            return@withContext Result.Error(Exception("ID de grupo familiar no válido."))
        }

        try {
            val alertsRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("sosAlerts")

            val newDocRef = alertsRef.document()
            val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val alert = SosAlert(
                id = newDocRef.id,
                familyGroupId = familyGroupId,
                disparadoPorUserId = userId,
                disparadoPorNombre = nombre,
                fecha = fechaActual,
                atendida = false,
                atendidaPorUserId = "",
                atendidaPorNombre = ""
            )

            newDocRef.set(alert).await()
            Log.d("SosRepository", "Alerta SOS registrada exitosamente con ID: ${newDocRef.id}")
            Result.Success(newDocRef.id)
        } catch (e: Exception) {
            handleFirebaseException("dispararAlerta", e)
            Result.Error(Exception("No se pudo enviar la alerta SOS. Verifique su conexión."))
        }
    }

    /**
     * Escucha en tiempo real las alertas activas (atendida == false) de un grupo familiar.
     */
    fun escucharAlertasActivas(familyGroupId: String): Flow<List<SosAlert>> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            Log.e("SosRepository", "escucharAlertasActivas: Usuario no autenticado")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            Log.e("SosRepository", "escucharAlertasActivas: ID de grupo inválido ($familyGroupId)")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("sosAlerts")
            .whereEqualTo("atendida", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("escucharAlertasActivas", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(SosAlert::class.java)
                    } catch (e: Exception) {
                        Log.e("SosRepository", "Error al deserializar SosAlert en tiempo real", e)
                        null
                    }
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Marca una alerta SOS como atendida por un usuario del grupo.
     */
    suspend fun marcarComoAtendida(
        familyGroupId: String,
        alertId: String,
        userId: String,
        nombre: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))
        }
        if (familyGroupId.isBlank() || alertId.isBlank()) {
            return@withContext Result.Error(Exception("Parámetros de alerta no válidos."))
        }

        try {
            val docRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("sosAlerts")
                .document(alertId)

            val updates = mapOf(
                "atendida" to true,
                "atendidaPorUserId" to userId,
                "atendidaPorNombre" to nombre
            )

            docRef.update(updates).await()
            Log.d("SosRepository", "Alerta SOS $alertId marcada como atendida por $nombre ($userId)")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("marcarComoAtendida", e)
            Result.Error(Exception("No se pudo marcar la alerta como atendida."))
        }
    }

    private fun handleFirebaseException(method: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e("SosRepository", "Firestore Error [$method]: Code=${e.code} - ${e.message}")
        } else if (e is FirebaseException) {
            Log.e("SosRepository", "Firebase Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        } else {
            Log.e("SosRepository", "General Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}

package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.AccionAuditoria
import com.example.cuidafamily.model.EntidadAuditoria
import com.example.cuidafamily.model.GroupMember
import com.example.cuidafamily.model.RegistroAuditoria
import com.example.cuidafamily.model.Role
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AuditoriaRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    /**
     * Requerimiento 07: Registrar una acción de auditoría en "familyGroups/{groupId}/auditoria/{id}".
     * Es SOLO de escritura: sin editar ni borrar.
     * Si la auditoría falla, NO rompe la operación principal.
     */
    suspend fun registrar(
        familyGroupId: String,
        entidad: EntidadAuditoria,
        accion: AccionAuditoria,
        entidadId: String,
        descripcion: String,
        detalleCambios: String,
        batch: WriteBatch? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null || familyGroupId.isBlank()) {
            Log.e("Auditoria", "No se pudo registrar auditoría: Sesión no válida o grupo vacío.")
            return@withContext Result.Error(Exception("Sesión no válida"))
        }

        try {
            val uid = user.uid

            // Obtener datos del miembro desde Firestore
            val memberDoc = try {
                db.collection("familyGroups")
                    .document(familyGroupId)
                    .collection("members")
                    .document(uid)
                    .get()
                    .await()
            } catch (e: Exception) {
                null
            }

            val userNombre = memberDoc?.getString("nombre")?.ifBlank { user.displayName } ?: user.displayName ?: "Usuario"
            val userRol = memberDoc?.getString("role") ?: Role.COLABORADOR.name

            val colRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("auditoria")

            val docRef = colRef.document()

            // Requerimiento PRIVACIDAD: Asegurar que la longitud del detalle no supere 500 caracteres
            val detalleSanitizado = if (detalleCambios.length > 500) detalleCambios.take(497) + "..." else detalleCambios

            val registro = RegistroAuditoria(
                id = docRef.id,
                familyGroupId = familyGroupId,
                entidad = entidad,
                accion = accion,
                entidadId = entidadId,
                descripcion = descripcion.trim(),
                detalleCambios = detalleSanitizado.trim(),
                userId = uid, // El campo del autor se llama exactamente "userId"
                userNombre = userNombre,
                userRol = userRol,
                fecha = System.currentTimeMillis()
            )

            if (batch != null) {
                batch.set(docRef, registro)
            } else {
                try {
                    docRef.set(registro).await()
                } catch (e: Exception) {
                    // Reintento único si falla
                    Log.w("Auditoria", "Reintentando guardado de auditoría ID=${docRef.id}...")
                    docRef.set(registro).await()
                }
            }

            Log.d("Auditoria", "Registrado: $entidad $accion por $userNombre")
            Result.Success(Unit)
        } catch (e: Exception) {
            // Si la auditoría falla, NO rompas la operación principal
            Log.e("Auditoria", "No se pudo registrar: ${e.message}")
            Result.Error(e)
        }
    }

    /**
     * Requerimiento 07: Consulta a Firestore SOLO por rango de fecha (Long), ordenada por fecha descendente y con limit(300).
     */
    suspend fun obtenerRegistros(
        familyGroupId: String,
        fechaDesdeMs: Long,
        fechaHastaMs: Long,
        limite: Int = 300
    ): Result<List<RegistroAuditoria>> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null || familyGroupId.isBlank()) {
            return@withContext Result.Error(Exception("Sesión no válida"))
        }

        try {
            val snapshot = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("auditoria")
                .whereGreaterThanOrEqualTo("fecha", fechaDesdeMs)
                .whereLessThanOrEqualTo("fecha", fechaHastaMs)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(limite.toLong())
                .get()
                .await()

            val lista = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(RegistroAuditoria::class.java)
                } catch (e: Exception) {
                    Log.e("Auditoria", "Error deserializando RegistroAuditoria ID=${doc.id}", e)
                    null
                }
            }

            Result.Success(lista)
        } catch (e: Exception) {
            Log.e("Auditoria", "Error consultando historial de auditoría: ${e.message}")
            Result.Error(Exception("No se pudo obtener el historial de movimientos."))
        }
    }
}

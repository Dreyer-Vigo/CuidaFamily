package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.DocumentoMedico
import com.example.cuidafamily.model.TipoDocumento
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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

// TODO: migrar a Firebase Storage para compartir archivos completos sin límite de tamaño.

object DocumentoRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    suspend fun subirDocumento(
        familyGroupId: String,
        patientId: String,
        documento: DocumentoMedico
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            Log.e("DocumentoRepository", "subirDocumento: Usuario no autenticado")
            return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))
        }

        if (familyGroupId.isBlank() || patientId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            return@withContext Result.Error(Exception("ID de grupo o paciente inválido."))
        }

        try {
            val docsRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("patients")
                .document(patientId)
                .collection("documentos")

            val docRef = if (documento.id.isNotBlank()) docsRef.document(documento.id) else docsRef.document()
            val fechaActual = if (documento.fechaSubida.isNotBlank()) documento.fechaSubida else SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val finalDoc = documento.copy(
                id = docRef.id,
                familyGroupId = familyGroupId,
                patientId = patientId,
                fechaSubida = fechaActual
            )

            docRef.set(finalDoc.toMap()).await()
            Log.d("DocumentoRepository", "Documento guardado con ID: ${docRef.id}")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.DOCUMENTO,
                accion = com.example.cuidafamily.model.AccionAuditoria.CREAR,
                entidadId = docRef.id,
                descripcion = "Subió documento: ${documento.nombre.ifBlank { "Sin título" }}",
                detalleCambios = "Tipo: ${documento.tipo.etiqueta}, Fecha: ${documento.fechaDocumento}"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("subirDocumento", e)
            Result.Error(Exception("No se pudo guardar el documento médico."))
        }
    }

    fun obtenerDocumentos(familyGroupId: String, patientId: String): Flow<List<DocumentoMedico>> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            Log.e("DocumentoRepository", "obtenerDocumentos: Usuario no autenticado")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        if (familyGroupId.isBlank() || patientId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            Log.e("DocumentoRepository", "obtenerDocumentos: Parámetros inválidos ($familyGroupId, $patientId)")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("patients")
            .document(patientId)
            .collection("documentos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("obtenerDocumentos", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toDocumentoMedico()
                }?.sortedByDescending { it.fechaDocumento } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun actualizarIndicaciones(
        familyGroupId: String,
        patientId: String,
        documentoId: String,
        nuevasIndicaciones: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            return@withContext Result.Error(Exception("Sesión no válida."))
        }

        try {
            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("patients")
                .document(patientId)
                .collection("documentos")
                .document(documentoId)
                .update("indicaciones", nuevasIndicaciones.trim())
                .await()

            Log.d("DocumentoRepository", "Indicaciones actualizadas para documento $documentoId")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("actualizarIndicaciones", e)
            Result.Error(Exception("No se pudieron actualizar las indicaciones."))
        }
    }

    suspend fun actualizarImagenesBase64(
        familyGroupId: String,
        patientId: String,
        documentoId: String,
        imagenesBase64: List<String>,
        paginasTotales: Int,
        paginasCompartidas: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val updates = mapOf(
                "imagenesBase64" to imagenesBase64,
                "paginasTotales" to paginasTotales,
                "paginasCompartidas" to paginasCompartidas
            )

            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("patients")
                .document(patientId)
                .collection("documentos")
                .document(documentoId)
                .update(updates)
                .await()

            Log.d("DocumentoRepository", "Imágenes Base64 actualizadas para documento $documentoId ($paginasCompartidas/$paginasTotales páginas)")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("actualizarImagenesBase64", e)
            Result.Error(Exception("No se pudo compartir la copia del archivo con el grupo."))
        }
    }

    suspend fun eliminarDocumento(
        familyGroupId: String,
        patientId: String,
        documentoId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) {
            return@withContext Result.Error(Exception("Sesión no válida."))
        }

        if (familyGroupId.isBlank() || patientId.isBlank() || documentoId.isBlank()) {
            return@withContext Result.Error(Exception("Parámetros de documento inválidos."))
        }

        try {
            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("patients")
                .document(patientId)
                .collection("documentos")
                .document(documentoId)
                .delete()
                .await()

            Log.d("DocumentoRepository", "Documento $documentoId eliminado correctamente")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.DOCUMENTO,
                accion = com.example.cuidafamily.model.AccionAuditoria.ELIMINAR,
                entidadId = documentoId,
                descripcion = "Eliminó un documento médico",
                detalleCambios = "Nombre y tipo de documento eliminado de Firestore (contenido no almacenado)"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("eliminarDocumento", e)
            Result.Error(Exception("No se pudo eliminar el documento médico."))
        }
    }

    private fun DocumentSnapshot.toDocumentoMedico(): DocumentoMedico? {
        return try {
            val patientId = getString("patientId") ?: ""
            val familyGroupId = getString("familyGroupId") ?: ""
            val tipoStr = getString("tipo") ?: ""
            val tipo = TipoDocumento.fromString(tipoStr)
            val nombre = getString("nombre") ?: ""
            val fechaDocumento = getString("fechaDocumento") ?: ""
            val fechaSubida = getString("fechaSubida") ?: ""
            val subidoPorUserId = getString("subidoPorUserId") ?: ""
            val subidoPorNombre = getString("subidoPorNombre") ?: ""
            val uriLocal = getString("uriLocal") ?: ""
            val mimeType = getString("mimeType") ?: ""
            val indicaciones = getString("indicaciones") ?: ""
            val imagenBase64 = getString("imagenBase64") ?: ""
            val imagenesBase64 = (get("imagenesBase64") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val paginasTotales = getLong("paginasTotales")?.toInt() ?: 0
            val paginasCompartidas = getLong("paginasCompartidas")?.toInt() ?: 0

            DocumentoMedico(
                id = id,
                patientId = patientId,
                familyGroupId = familyGroupId,
                tipo = tipo,
                nombre = nombre,
                fechaDocumento = fechaDocumento,
                fechaSubida = fechaSubida,
                subidoPorUserId = subidoPorUserId,
                subidoPorNombre = subidoPorNombre,
                uriLocal = uriLocal,
                mimeType = mimeType,
                indicaciones = indicaciones,
                imagenBase64 = imagenBase64,
                imagenesBase64 = imagenesBase64,
                paginasTotales = paginasTotales,
                paginasCompartidas = paginasCompartidas
            )
        } catch (e: Exception) {
            Log.e("DocumentoRepository", "Error deserializando DocumentoMedico ${id}", e)
            null
        }
    }

    private fun DocumentoMedico.toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "patientId" to patientId,
            "familyGroupId" to familyGroupId,
            "tipo" to tipo.name,
            "nombre" to nombre,
            "fechaDocumento" to fechaDocumento,
            "fechaSubida" to fechaSubida,
            "subidoPorUserId" to subidoPorUserId,
            "subidoPorNombre" to subidoPorNombre,
            "uriLocal" to uriLocal,
            "mimeType" to mimeType,
            "indicaciones" to indicaciones,
            "imagenBase64" to imagenBase64,
            "imagenesBase64" to imagenesBase64,
            "paginasTotales" to paginasTotales,
            "paginasCompartidas" to paginasCompartidas
        )
    }

    private fun handleFirebaseException(method: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e("DocumentoRepository", "Firestore Error [$method]: Code=${e.code} - ${e.message}")
        } else if (e is FirebaseException) {
            Log.e("DocumentoRepository", "Firebase Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        } else {
            Log.e("DocumentoRepository", "General Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}

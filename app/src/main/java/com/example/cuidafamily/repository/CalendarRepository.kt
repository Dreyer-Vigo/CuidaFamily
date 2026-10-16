package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.CalendarEvent
import com.example.cuidafamily.model.EstadoConfirmacion
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.TipoEvento
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalTime

object CalendarRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    private val _allEventsFlow = MutableStateFlow<Map<String, CalendarEvent>>(emptyMap())
    val allEventsFlow: StateFlow<Map<String, CalendarEvent>> = _allEventsFlow.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    fun monitorizarEventosDelGrupo(familyGroupId: String) {
        val user = auth.currentUser
        if (user == null) {
            Log.e("CalendarRepository", "monitorizarEventosDelGrupo: Usuario no autenticado")
            return
        }

        listenerRegistration?.remove()
        
        listenerRegistration = db.collection("familyGroups").document(familyGroupId)
            .collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("monitorizarEventos", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val events = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toCalendarEvent()
                        } catch (e: Exception) {
                            Log.e("CalendarRepository", "Error deserializing CalendarEvent: ${doc.id}", e)
                            null
                        }
                    }.associateBy { it.id }
                    _allEventsFlow.update { events }
                }
            }
    }

    suspend fun crearEvento(event: CalendarEvent): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))

        try {
            db.collection("familyGroups").document(event.familyGroupId)
                .collection("events").document(event.id)
                .set(event.toFirestoreMap())
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("crearEvento", e)
            Result.Error(Exception("Error al agendar la actividad."))
        }
    }

    suspend fun editarEvento(event: CalendarEvent): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups").document(event.familyGroupId)
                .collection("events").document(event.id)
                .set(event.toFirestoreMap())
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("editarEvento", e)
            Result.Error(Exception("Error al actualizar la actividad."))
        }
    }

    suspend fun eliminarEvento(familyGroupId: String, eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups").document(familyGroupId)
                .collection("events").document(eventId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("eliminarEvento", e)
            Result.Error(Exception("No se pudo eliminar el evento."))
        }
    }

    fun obtenerEventosDelMesRealTime(familyGroupId: String, mes: Int, anio: Int): Flow<List<CalendarEvent>> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            close(Exception("Sesión no válida"))
            return@callbackFlow
        }

        val prefijoMes = String.format(java.util.Locale.US, "%04d-%02d", anio, mes)
        
        val listener = db.collection("familyGroups").document(familyGroupId)
            .collection("events")
            .whereGreaterThanOrEqualTo("fecha", "$prefijoMes-01")
            .whereLessThanOrEqualTo("fecha", "$prefijoMes-31")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("obtenerEventosDelMesRealTime", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val list = snapshot?.documents?.mapNotNull { it.toCalendarEvent() } ?: emptyList()
                trySend(list.sortedWith(compareBy({ it.fecha }, { it.horaInicio })))
            }
        
        awaitClose { listener.remove() }
    }

    fun obtenerEventosDelDiaRealTime(familyGroupId: String, fecha: String): Flow<List<CalendarEvent>> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            close(Exception("Sesión no válida"))
            return@callbackFlow
        }

        val listener = db.collection("familyGroups").document(familyGroupId)
            .collection("events")
            .whereEqualTo("fecha", fecha)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("obtenerEventosDelDiaRealTime", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val list = snapshot?.documents?.mapNotNull { it.toCalendarEvent() } ?: emptyList()
                trySend(list.sortedBy { it.horaInicio })
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun obtenerEventoPorId(familyGroupId: String, eventId: String): CalendarEvent? = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext null
        if (familyGroupId.isBlank() || eventId.isBlank()) return@withContext null

        try {
            val doc = db.collection("familyGroups").document(familyGroupId)
                .collection("events").document(eventId)
                .get().await()
            doc.toCalendarEvent()
        } catch (e: Exception) {
            handleFirebaseException("obtenerEventoPorId", e)
            null
        }
    }

    suspend fun obtenerEventosDelMes(familyGroupId: String, mes: Int, anio: Int): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))
        try {
            val prefijoMes = String.format(java.util.Locale.US, "%04d-%02d", anio, mes)
            val query = db.collection("familyGroups").document(familyGroupId)
                .collection("events")
                .whereGreaterThanOrEqualTo("fecha", "$prefijoMes-01")
                .whereLessThanOrEqualTo("fecha", "$prefijoMes-31")
                .get().await()
            val list = query.documents.mapNotNull { it.toCalendarEvent() }.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
            Result.Success(list)
        } catch (e: Exception) {
            handleFirebaseException("obtenerEventosDelMes", e)
            Result.Error(Exception("Error al cargar mes"))
        }
    }

    suspend fun obtenerEventosDelDia(familyGroupId: String, fecha: String): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))
        try {
            val query = db.collection("familyGroups").document(familyGroupId)
                .collection("events")
                .whereEqualTo("fecha", fecha)
                .get().await()
            val list = query.documents.mapNotNull { it.toCalendarEvent() }.sortedBy { it.horaInicio }
            Result.Success(list)
        } catch (e: Exception) {
            handleFirebaseException("obtenerEventosDelDia", e)
            Result.Error(Exception("Error al cargar día"))
        }
    }

    suspend fun marcarComoCompletado(familyGroupId: String, eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups").document(familyGroupId)
                .collection("events").document(eventId)
                .update("completado", true)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("marcarComoCompletado", e)
            Result.Error(Exception("No se pudo completar la tarea."))
        }
    }

    suspend fun actualizarConfirmacionAsistencia(
        familyGroupId: String,
        eventId: String,
        estado: EstadoConfirmacion,
        motivo: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val updates = mapOf(
                "confirmacionAsistencia" to estado.name,
                "motivoRechazo" to motivo
            )
            db.collection("familyGroups").document(familyGroupId)
                .collection("events").document(eventId)
                .update(updates)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("actualizarConfirmacionAsistencia", e)
            Result.Error(Exception("No se pudo actualizar la confirmación de asistencia."))
        }
    }

    suspend fun reasignarEvento(
        familyGroupId: String,
        eventId: String,
        nuevoUserId: String,
        nuevoNombre: String,
        nuevoRol: Role?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val updates = mapOf(
                "asignadoAUserId" to nuevoUserId,
                "asignadoANombre" to nuevoNombre,
                "asignadoARol" to nuevoRol?.name,
                "confirmacionAsistencia" to EstadoConfirmacion.PENDIENTE.name,
                "motivoRechazo" to ""
            )
            db.collection("familyGroups").document(familyGroupId)
                .collection("events").document(eventId)
                .update(updates)
                .await()

            Log.d("CalendarRepository", "Evento $eventId reasignado a $nuevoNombre ($nuevoUserId)")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("reasignarEvento", e)
            Result.Error(Exception("No se pudo reasignar el evento."))
        }
    }

    private fun handleFirebaseException(method: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e("CalendarRepository", "Firestore Error [$method]: Code=${e.code} - ${e.message}")
        } else if (e is FirebaseException) {
            Log.e("CalendarRepository", "Firebase Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        } else {
            Log.e("CalendarRepository", "General Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    private fun DocumentSnapshot.toCalendarEvent(): CalendarEvent? {
        return try {
            val confirmacionStr = getString("confirmacionAsistencia")
            val confirmacion = EstadoConfirmacion.fromString(confirmacionStr)
            val motivoRechazo = getString("motivoRechazo") ?: ""
            val lugar = getString("lugar") ?: ""
            val fotoUrl = getString("fotoUrl") ?: ""
            val rolAsignado = getString("rolAsignado") ?: ""
            val fechaLimiteRespuesta = getLong("fechaLimiteRespuesta")

            CalendarEvent(
                id = id,
                titulo = getString("titulo") ?: "",
                tipo = TipoEvento.fromString(getString("tipo")),
                fecha = getString("fecha") ?: "",
                horaInicio = LocalTime.parse(getString("horaInicio")),
                horaFin = LocalTime.parse(getString("horaFin")),
                asignadoAUserId = getString("asignadoAUserId"),
                asignadoANombre = getString("asignadoANombre"),
                asignadoARol = Role.fromString(getString("asignadoARol")),
                creadoPorUserId = getString("creadoPorUserId") ?: "",
                notas = getString("notas"),
                completado = getBoolean("completado") ?: false,
                familyGroupId = getString("familyGroupId") ?: "",
                confirmacionAsistencia = confirmacion,
                motivoRechazo = motivoRechazo,
                lugar = lugar,
                fotoUrl = fotoUrl,
                rolAsignado = rolAsignado,
                fechaLimiteRespuesta = fechaLimiteRespuesta
            )
        } catch (e: Exception) { null }
    }

    private fun CalendarEvent.toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "titulo" to titulo,
            "tipo" to tipo.name,
            "fecha" to fecha,
            "horaInicio" to horaInicio.toString(),
            "horaFin" to horaFin.toString(),
            "asignadoAUserId" to asignadoAUserId,
            "asignadoANombre" to asignadoANombre,
            "asignadoARol" to asignadoARol?.name,
            "creadoPorUserId" to creadoPorUserId,
            "notas" to notas,
            "completado" to completado,
            "familyGroupId" to familyGroupId,
            "confirmacionAsistencia" to confirmacionAsistencia.name,
            "motivoRechazo" to motivoRechazo,
            "lugar" to lugar,
            "fotoUrl" to fotoUrl,
            "rolAsignado" to rolAsignado,
            "fechaLimiteRespuesta" to fechaLimiteRespuesta
        )
    }

    fun limpiarMemoria() {
        listenerRegistration?.remove()
        _allEventsFlow.update { emptyMap() }
    }
}

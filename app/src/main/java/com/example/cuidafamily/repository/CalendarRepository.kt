package com.example.cuidafamily.repository

import android.util.Log
import com.example.cuidafamily.model.CalendarEvent
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

/**
 * Repositorio de agenda para la gestión colaborativa de los Eventos del Calendario en Firestore.
 * Estructura: familyGroups/{groupId}/events/{eventId}
 */
object CalendarRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    private val _allEventsFlow = MutableStateFlow<Map<String, CalendarEvent>>(emptyMap())
    val allEventsFlow: StateFlow<Map<String, CalendarEvent>> = _allEventsFlow.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Activa la escucha en tiempo real para un grupo familiar específico.
     */
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

    /**
     * Inserta un nuevo evento programado en Firestore.
     */
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

    /**
     * Actualiza un evento preexistente.
     */
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

    /**
     * Elimina físicamente un evento del calendario.
     */
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

    /**
     * Obtiene de forma asíncrona la lista de eventos filtrados por mes y año con escucha en tiempo real.
     */
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

    /**
     * Obtiene los eventos de un día específico en tiempo real.
     */
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

    /**
     * Versiones suspendidas (mantenidas por compatibilidad temporal si es necesario).
     */
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

    /**
     * Conmutador para finalizar una tarea con update() parcial.
     */
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
                familyGroupId = getString("familyGroupId") ?: ""
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
            "familyGroupId" to familyGroupId
        )
    }

    fun limpiarMemoria() {
        listenerRegistration?.remove()
        _allEventsFlow.update { emptyMap() }
    }
}

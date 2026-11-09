package com.example.cuidafamily.repository

import android.location.Location
import android.util.Log
import com.example.cuidafamily.model.EstadoConfirmacion
import com.example.cuidafamily.model.FamilyGroup
import com.example.cuidafamily.model.Turno
import com.example.cuidafamily.model.TurnoCheckIn
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TurnoRepository {

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    suspend fun crearTurno(familyGroupId: String, turno: Turno): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida, vuelve a iniciar sesión."))
        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") return@withContext Result.Error(Exception("ID de grupo no válido."))

        val startTime = System.currentTimeMillis()
        try {
            val collectionRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnos")

            val docRef = if (turno.id.isNotBlank()) collectionRef.document(turno.id) else collectionRef.document()
            val finalTurno = turno.copy(id = docRef.id, familyGroupId = familyGroupId)

            docRef.set(finalTurno).await()
            val endTime = System.currentTimeMillis()
            Log.d("TurnoRepository", "crearTurno inicio: $startTime, fin: $endTime, total: ${endTime - startTime}ms - ID: ${docRef.id}")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.TURNO,
                accion = com.example.cuidafamily.model.AccionAuditoria.CREAR,
                entidadId = docRef.id,
                descripcion = "Asignó turno a ${finalTurno.cuidadorNombre}",
                detalleCambios = "Horario: ${finalTurno.horaInicio}-${finalTurno.horaFin}"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("crearTurno", e)
            Result.Error(Exception("No se pudo guardar el turno."))
        }
    }

    suspend fun editarTurno(
        familyGroupId: String,
        turnoAnterior: Turno?,
        nuevoTurno: Turno
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val cambios = mutableListOf<String>()
            if (turnoAnterior != null) {
                if (turnoAnterior.cuidadorUserId != nuevoTurno.cuidadorUserId) {
                    cambios.add("Cuidador: ${turnoAnterior.cuidadorNombre} → ${nuevoTurno.cuidadorNombre}")
                }
                if (turnoAnterior.diasSemana != nuevoTurno.diasSemana) {
                    cambios.add("Días de trabajo modificados")
                }
                if (turnoAnterior.horaInicio != nuevoTurno.horaInicio || turnoAnterior.horaFin != nuevoTurno.horaFin) {
                    cambios.add("Horario: ${turnoAnterior.horaInicio}-${turnoAnterior.horaFin} → ${nuevoTurno.horaInicio}-${nuevoTurno.horaFin}")
                }
                if (turnoAnterior.fechaInicio != nuevoTurno.fechaInicio || turnoAnterior.fechaFin != nuevoTurno.fechaFin) {
                    cambios.add("Rango de fechas modificado")
                }
                if (turnoAnterior.tareas != nuevoTurno.tareas) {
                    cambios.add("Tareas modificadas")
                }
            } else {
                cambios.add("Turno actualizado")
            }

            val reinicia = cambios.isNotEmpty()
            val avisoTexto = if (cambios.isNotEmpty()) cambios.joinToString("; ") else ""

            Log.d("TurnoEdicion", "Editando turno ${nuevoTurno.id}, cambios relevantes=$cambios, reinicia confirmación=$reinicia")

            val finalTurno = if (reinicia) {
                nuevoTurno.copy(
                    confirmacion = EstadoConfirmacion.PENDIENTE,
                    motivoRechazo = "",
                    respuestaVistaPorAdmin = true,
                    fechaUltimaModificacion = System.currentTimeMillis(),
                    avisoParaCuidador = avisoTexto
                )
            } else {
                nuevoTurno.copy(
                    fechaUltimaModificacion = System.currentTimeMillis()
                )
            }

            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnos")
                .document(finalTurno.id)
                .set(finalTurno, SetOptions.merge())
                .await()

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.TURNO,
                accion = com.example.cuidafamily.model.AccionAuditoria.MODIFICAR,
                entidadId = finalTurno.id,
                descripcion = "Editó el turno de ${finalTurno.cuidadorNombre}",
                detalleCambios = if (cambios.isNotEmpty()) cambios.joinToString("; ") else "Turno modificado"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("editarTurno", e)
            Result.Error(Exception("No se pudo actualizar el turno."))
        }
    }

    suspend fun eliminarTurno(familyGroupId: String, turnoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val updates = mapOf(
                "activo" to false,
                "avisoParaCuidador" to "🗑️ Turno cancelado por el Administrador",
                "fechaUltimaModificacion" to System.currentTimeMillis()
            )

            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnos")
                .document(turnoId)
                .update(updates)
                .await()

            Log.d("TurnoRepository", "Turno $turnoId cancelado por admin")

            AuditoriaRepository.registrar(
                familyGroupId = familyGroupId,
                entidad = com.example.cuidafamily.model.EntidadAuditoria.TURNO,
                accion = com.example.cuidafamily.model.AccionAuditoria.ELIMINAR,
                entidadId = turnoId,
                descripcion = "Desactivó un turno asignado",
                detalleCambios = "Turno desactivado por el Administrador"
            )

            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("eliminarTurno", e)
            Result.Error(Exception("No se pudo desactivar el turno."))
        }
    }

    suspend fun borrarTurnoDefinitivo(familyGroupId: String, turnoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnos")
                .document(turnoId)
                .delete()
                .await()

            Log.d("TurnoRepository", "Turno $turnoId eliminado definitivamente de Firestore")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("borrarTurnoDefinitivo", e)
            Result.Error(Exception("No se pudo borrar el documento del turno."))
        }
    }

    suspend fun actualizarConfirmacionTurno(
        familyGroupId: String,
        turnoId: String,
        estado: EstadoConfirmacion,
        motivo: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val updates = mutableMapOf<String, Any>(
                "confirmacion" to estado.name,
                "motivoRechazo" to motivo,
                "avisoParaCuidador" to ""
            )
            if (estado != EstadoConfirmacion.PENDIENTE) {
                updates["confirmacionFecha"] = System.currentTimeMillis()
                updates["respuestaVistaPorAdmin"] = false
            } else {
                updates["respuestaVistaPorAdmin"] = true
            }

            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnos")
                .document(turnoId)
                .update(updates)
                .await()

            Log.d("TurnoRepository", "Confirmación de turno $turnoId actualizada a $estado con motivo '$motivo'")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("actualizarConfirmacionTurno", e)
            Result.Error(Exception("No se pudo responder a la asignación del turno."))
        }
    }

    suspend fun reasignarTurno(
        familyGroupId: String,
        turnoId: String,
        nuevoCuidadorUserId: String,
        nuevoCuidadorNombre: String,
        nuevoCuidadorTelefono: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val updates = mapOf(
                "cuidadorUserId" to nuevoCuidadorUserId,
                "cuidadorNombre" to nuevoCuidadorNombre,
                "cuidadorTelefono" to nuevoCuidadorTelefono,
                "confirmacion" to EstadoConfirmacion.PENDIENTE.name,
                "motivoRechazo" to "",
                "respuestaVistaPorAdmin" to true,
                "fechaUltimaModificacion" to System.currentTimeMillis()
            )

            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnos")
                .document(turnoId)
                .update(updates)
                .await()

            Log.d("TurnoRepository", "Turno $turnoId reasignado a $nuevoCuidadorNombre ($nuevoCuidadorUserId)")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("reasignarTurno", e)
            Result.Error(Exception("No se pudo reasignar el turno."))
        }
    }

    suspend fun marcarRespuestaVista(familyGroupId: String, turnoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnos")
                .document(turnoId)
                .update("respuestaVistaPorAdmin", true)
                .await()

            Log.d("TurnoRepository", "Respuesta del turno $turnoId marcada como vista por admin")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("marcarRespuestaVista", e)
            Result.Error(Exception("No se pudo marcar la respuesta como vista."))
        }
    }

    suspend fun marcarTodasRespuestasVistas(familyGroupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val query = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnos")
                .whereEqualTo("respuestaVistaPorAdmin", false)
                .get()
                .await()

            val batch = db.batch()
            for (doc in query.documents) {
                batch.update(doc.reference, "respuestaVistaPorAdmin", true)
            }
            batch.commit().await()

            Log.d("TurnoRepository", "Todas las respuestas de turnos marcadas como vistas")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("marcarTodasRespuestasVistas", e)
            Result.Error(Exception("Error al marcar respuestas como vistas."))
        }
    }

    fun obtenerTodosLosTurnos(familyGroupId: String): Flow<List<Turno>> = callbackFlow {
        val user = auth.currentUser
        if (user == null || familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("turnos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("obtenerTodosLosTurnos", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Turno::class.java)
                    } catch (e: Exception) {
                        Log.e("TurnoRepository", "Error deserializando Turno ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    fun obtenerTurnosActivos(familyGroupId: String): Flow<List<Turno>> = obtenerTodosLosTurnos(familyGroupId)

    suspend fun configurarDomicilio(familyGroupId: String, lat: Double, lng: Double): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val updates = mapOf(
                "domicilioLat" to lat,
                "domicilioLng" to lng
            )

            db.collection("familyGroups")
                .document(familyGroupId)
                .update(updates)
                .await()

            Log.d("TurnoRepository", "Domicilio configurado: ($lat, $lng)")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("configurarDomicilio", e)
            Result.Error(Exception("No se pudo guardar la ubicación del domicilio."))
        }
    }

    suspend fun registrarCheckIn(
        familyGroupId: String,
        turnoId: String,
        cuidadorUserId: String,
        latActual: Double,
        lngActual: Double
    ): Result<TurnoCheckIn> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val groupDoc = db.collection("familyGroups").document(familyGroupId).get().await()
            val familyGroup = groupDoc.toObject(FamilyGroup::class.java)

            val domLat = familyGroup?.domicilioLat
            val domLng = familyGroup?.domicilioLng

            if (domLat == null || domLng == null) {
                return@withContext Result.Error(Exception("El Administrador aún no ha configurado el domicilio de referencia."))
            }

            val results = FloatArray(1)
            Location.distanceBetween(latActual, lngActual, domLat, domLng, results)
            val distanciaMetros = results[0]
            val ubicacionValida = distanciaMetros <= 150f

            val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val horaStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            val checkInRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnoCheckIns")
                .document()

            val checkIn = TurnoCheckIn(
                id = checkInRef.id,
                turnoId = turnoId,
                cuidadorUserId = cuidadorUserId,
                fecha = hoyStr,
                horaCheckIn = horaStr,
                ubicacionValida = ubicacionValida,
                latitudCapturada = latActual,
                longitudCapturada = lngActual
            )

            checkInRef.set(checkIn).await()
            Log.d("TurnoRepository", "CheckIn registrado: $checkIn (Distancia: ${distanciaMetros.toInt()}m)")
            Result.Success(checkIn)
        } catch (e: Exception) {
            handleFirebaseException("registrarCheckIn", e)
            Result.Error(Exception("No se pudo registrar la entrada por GPS."))
        }
    }

    fun obtenerCheckInsDeHoy(familyGroupId: String): Flow<List<TurnoCheckIn>> = callbackFlow {
        val user = auth.currentUser
        if (user == null || familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("turnoCheckIns")
            .whereEqualTo("fecha", hoyStr)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("obtenerCheckInsDeHoy", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(TurnoCheckIn::class.java)
                    } catch (e: Exception) {
                        Log.e("TurnoRepository", "Error deserializando TurnoCheckIn", e)
                        null
                    }
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun crearSolicitudIntercambio(
        familyGroupId: String,
        solicitud: com.example.cuidafamily.model.SolicitudIntercambio,
        turno: Turno
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))
        if (solicitud.motivo.isBlank()) return@withContext Result.Error(Exception("El motivo es obligatorio."))

        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (solicitud.fecha < hoyStr) {
            return@withContext Result.Error(Exception("No se puede solicitar cambio para una fecha pasada."))
        }

        if (!turno.aplicaEnFecha(solicitud.fecha)) {
            return@withContext Result.Error(Exception("El turno no aplica en la fecha seleccionada."))
        }

        try {
            val colRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("solicitudesIntercambio")

            val pendientes = colRef
                .whereEqualTo("turnoId", solicitud.turnoId)
                .whereEqualTo("fecha", solicitud.fecha)
                .whereEqualTo("estado", com.example.cuidafamily.model.EstadoSolicitud.PENDIENTE.name)
                .get()
                .await()

            if (!pendientes.isEmpty) {
                return@withContext Result.Error(Exception("Ya existe una solicitud pendiente de cambio para este turno en esa fecha."))
            }

            val docRef = colRef.document()
            val finalSolicitud = solicitud.copy(
                id = docRef.id,
                familyGroupId = familyGroupId,
                estado = com.example.cuidafamily.model.EstadoSolicitud.PENDIENTE,
                fechaCreacion = System.currentTimeMillis(),
                vistaPorAdmin = false,
                vistaPorSolicitante = true
            )

            docRef.set(finalSolicitud).await()
            Log.d("Intercambio", "Solicitud creada id=${docRef.id} turno=${solicitud.turnoId} fecha=${solicitud.fecha} por ${solicitud.solicitanteNombre}")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("crearSolicitudIntercambio", e)
            Result.Error(Exception("No se pudo enviar la solicitud de cambio."))
        }
    }

    suspend fun cancelarSolicitudIntercambio(familyGroupId: String, solicitudId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("solicitudesIntercambio")
                .document(solicitudId)
                .update("estado", com.example.cuidafamily.model.EstadoSolicitud.CANCELADA.name)
                .await()

            Log.d("Intercambio", "Solicitud $solicitudId cancelada por el solicitante")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("cancelarSolicitudIntercambio", e)
            Result.Error(Exception("No se pudo cancelar la solicitud."))
        }
    }

    suspend fun aprobarSolicitudIntercambio(
        familyGroupId: String,
        solicitud: com.example.cuidafamily.model.SolicitudIntercambio,
        reemplazoUserId: String,
        reemplazoNombre: String,
        adminUserId: String,
        adminNombre: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))
        if (reemplazoUserId.isBlank()) return@withContext Result.Error(Exception("Debes seleccionar un cuidador de reemplazo."))

        try {
            val yyyyMMdd = solicitud.fecha.replace("-", "")
            val excId = "${solicitud.turnoId}_$yyyyMMdd"

            val batch = db.batch()

            val solRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("solicitudesIntercambio")
                .document(solicitud.id)

            val updatesSol = mapOf(
                "estado" to com.example.cuidafamily.model.EstadoSolicitud.APROBADA.name,
                "reemplazoPropuestoUserId" to reemplazoUserId,
                "reemplazoPropuestoNombre" to reemplazoNombre,
                "resueltaPorUserId" to adminUserId,
                "resueltaPorNombre" to adminNombre,
                "fechaResolucion" to System.currentTimeMillis(),
                "vistaPorSolicitante" to false
            )
            batch.update(solRef, updatesSol)

            val excRef = db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnoExcepciones")
                .document(excId)

            val excepcion = com.example.cuidafamily.model.ExcepcionTurno(
                id = excId,
                familyGroupId = familyGroupId,
                turnoId = solicitud.turnoId,
                fecha = solicitud.fecha,
                cuidadorOriginalUserId = solicitud.solicitanteUserId,
                cuidadorOriginalNombre = solicitud.solicitanteNombre,
                cuidadorReemplazoUserId = reemplazoUserId,
                cuidadorReemplazoNombre = reemplazoNombre,
                solicitudId = solicitud.id,
                confirmacion = EstadoConfirmacion.PENDIENTE,
                vistaPorReemplazo = false
            )
            batch.set(excRef, excepcion, SetOptions.merge())

            batch.commit().await()

            Log.d("Intercambio", "Solicitud ${solicitud.id} aprobada por $adminNombre. Excepción $excId creada para $reemplazoNombre")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("aprobarSolicitudIntercambio", e)
            Result.Error(Exception("No se pudo aprobar la solicitud de cambio."))
        }
    }

    suspend fun rechazarSolicitudIntercambio(
        familyGroupId: String,
        solicitudId: String,
        motivoRechazo: String,
        adminUserId: String,
        adminNombre: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val updates = mapOf(
                "estado" to com.example.cuidafamily.model.EstadoSolicitud.RECHAZADA.name,
                "motivoRechazo" to motivoRechazo.trim(),
                "resueltaPorUserId" to adminUserId,
                "resueltaPorNombre" to adminNombre,
                "fechaResolucion" to System.currentTimeMillis(),
                "vistaPorSolicitante" to false
            )

            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("solicitudesIntercambio")
                .document(solicitudId)
                .update(updates)
                .await()

            Log.d("Intercambio", "Solicitud $solicitudId rechazada por $adminNombre: $motivoRechazo")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("rechazarSolicitudIntercambio", e)
            Result.Error(Exception("No se pudo rechazar la solicitud."))
        }
    }

    suspend fun responderExcepcionTurno(
        familyGroupId: String,
        excepcionId: String,
        confirmacion: EstadoConfirmacion
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            val updates = mapOf(
                "confirmacion" to confirmacion.name,
                "vistaPorReemplazo" to true
            )

            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnoExcepciones")
                .document(excepcionId)
                .update(updates)
                .await()

            Log.d("Intercambio", "Excepción $excepcionId respuesta: $confirmacion")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("responderExcepcionTurno", e)
            Result.Error(Exception("No se pudo responder al reemplazo de turno."))
        }
    }

    suspend fun marcarSolicitudVistaPorSolicitante(familyGroupId: String, solicitudId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("solicitudesIntercambio")
                .document(solicitudId)
                .update("vistaPorSolicitante", true)
                .await()

            Log.d("Intercambio", "Solicitud $solicitudId marcada como vista por solicitante")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("marcarSolicitudVistaPorSolicitante", e)
            Result.Error(Exception("No se pudo marcar solicitud como vista."))
        }
    }

    suspend fun marcarExcepcionVistaPorReemplazo(familyGroupId: String, excepcionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        if (user == null) return@withContext Result.Error(Exception("Sesión no válida."))

        try {
            db.collection("familyGroups")
                .document(familyGroupId)
                .collection("turnoExcepciones")
                .document(excepcionId)
                .update("vistaPorReemplazo", true)
                .await()

            Log.d("Intercambio", "Excepción $excepcionId marcada como vista por reemplazo")
            Result.Success(Unit)
        } catch (e: Exception) {
            handleFirebaseException("marcarExcepcionVistaPorReemplazo", e)
            Result.Error(Exception("No se pudo marcar excepción como vista."))
        }
    }

    fun obtenerSolicitudesIntercambio(familyGroupId: String): Flow<List<com.example.cuidafamily.model.SolicitudIntercambio>> = callbackFlow {
        val user = auth.currentUser
        if (user == null || familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("solicitudesIntercambio")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("obtenerSolicitudesIntercambio", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.cuidafamily.model.SolicitudIntercambio::class.java)
                    } catch (e: Exception) {
                        Log.e("TurnoRepository", "Error deserializando SolicitudIntercambio", e)
                        null
                    }
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    fun obtenerExcepcionesTurno(familyGroupId: String): Flow<List<com.example.cuidafamily.model.ExcepcionTurno>> = callbackFlow {
        val user = auth.currentUser
        if (user == null || familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("familyGroups")
            .document(familyGroupId)
            .collection("turnoExcepciones")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirebaseException("obtenerExcepcionesTurno", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.cuidafamily.model.ExcepcionTurno::class.java)
                    } catch (e: Exception) {
                        Log.e("TurnoRepository", "Error deserializando ExcepcionTurno", e)
                        null
                    }
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    private fun handleFirebaseException(method: String, e: Exception) {
        if (e is FirebaseFirestoreException) {
            Log.e("TurnoRepository", "Firestore Error [$method]: Code=${e.code} - ${e.message}")
        } else if (e is FirebaseException) {
            Log.e("TurnoRepository", "Firebase Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        } else {
            Log.e("TurnoRepository", "General Error [$method]: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}

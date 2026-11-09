package com.example.cuidafamily.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.cuidafamily.MainActivity
import com.example.cuidafamily.model.CalendarEvent
import com.example.cuidafamily.model.EstadoAnimo
import com.example.cuidafamily.model.EstadoConfirmacion
import com.example.cuidafamily.model.Turno
import com.example.cuidafamily.repository.BitacoraRepository
import com.example.cuidafamily.repository.CalendarRepository
import com.example.cuidafamily.repository.SosRepository
import com.example.cuidafamily.repository.TurnoRepository
import com.example.cuidafamily.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class CuidaFamilyMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var sosJob: Job? = null
    private var bitacoraJob: Job? = null
    private var turnosJob: Job? = null
    private var eventosJob: Job? = null
    private var solicitudesJob: Job? = null
    private var excepcionesJob: Job? = null

    private var currentGroupId: String? = null
    private var currentUserId: String? = null

    private var lastNotifiedSosId: String? = null
    private var isBitacoraInitialLoad = true
    private var previousBitacoraEntryIds = setOf<String>()

    private var isTurnosInitialLoad = true
    private var previousTurnosMap = mapOf<String, Turno>()

    private var isEventosInitialLoad = true
    private var previousEventosMap = mapOf<String, CalendarEvent>()

    private var isSolicitudesInitialLoad = true
    private var previousSolicitudesMap = mapOf<String, com.example.cuidafamily.model.SolicitudIntercambio>()

    private var isExcepcionesInitialLoad = true
    private var previousExcepcionesMap = mapOf<String, com.example.cuidafamily.model.ExcepcionTurno>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val groupId = intent?.getStringExtra(EXTRA_GROUP_ID) ?: ""
        val userId = intent?.getStringExtra(EXTRA_USER_ID) ?: ""
        val userName = intent?.getStringExtra(EXTRA_USER_NAME) ?: ""
        val patientNombre = intent?.getStringExtra(EXTRA_PATIENT_NOMBRE) ?: "el adulto mayor"

        if (groupId.isBlank() || userId.isBlank()) {
            Log.w(TAG, "onStartCommand: Parámetros inválidos, deteniendo servicio.")
            stopSelf()
            return START_NOT_STICKY
        }

        NotificationHelper.inicializarCanales(this)
        iniciarForegroundNotification()

        if (groupId != currentGroupId || userId != currentUserId) {
            currentGroupId = groupId
            currentUserId = userId
            isBitacoraInitialLoad = true
            previousBitacoraEntryIds = emptySet()
            isTurnosInitialLoad = true
            previousTurnosMap = emptyMap()
            isEventosInitialLoad = true
            previousEventosMap = emptyMap()

            iniciarListeners(groupId, userId, userName, patientNombre)
        }

        return START_STICKY
    }

    private fun iniciarForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            SERVICE_NOTIFICATION_ID,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.SERVICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("CuidaFamily Activo")
            .setContentText("CuidaFamily está conectado a tu grupo familiar")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, notification)
        }
    }

    private fun iniciarListeners(
        familyGroupId: String,
        userId: String,
        userName: String,
        patientNombre: String
    ) {
        sosJob?.cancel()
        sosJob = serviceScope.launch {
            SosRepository.escucharAlertasActivas(familyGroupId).collectLatest { alerts ->
                val incomingAlert = alerts.firstOrNull { it.disparadoPorUserId != userId && !it.atendida }
                if (incomingAlert != null && incomingAlert.id != lastNotifiedSosId) {
                    lastNotifiedSosId = incomingAlert.id
                    Log.d(TAG, "Servicio detectó alerta SOS de: ${incomingAlert.disparadoPorNombre}")
                    NotificationHelper.mostrarNotificacionSos(
                        context = this@CuidaFamilyMonitorService,
                        alertId = incomingAlert.id,
                        nombreUsuario = incomingAlert.disparadoPorNombre,
                        fecha = incomingAlert.fecha
                    )
                    NotificationHelper.dispararVibracionSos(this@CuidaFamilyMonitorService)
                }
            }
        }

        bitacoraJob?.cancel()
        bitacoraJob = serviceScope.launch {
            BitacoraRepository.obtenerEntradas(familyGroupId).collectLatest { lista ->
                val newEntryIds = lista.map { it.id }.toSet()

                if (!isBitacoraInitialLoad) {
                    val addedEntries = lista.filter { it.id !in previousBitacoraEntryIds && it.autorUserId != userId }
                    for (entry in addedEntries) {
                        Log.d(TAG, "Servicio detectó novedad en Bitácora de: ${entry.autorNombre}")
                        val animoStr = if (entry.estadoAnimo != EstadoAnimo.NINGUNO) "${entry.estadoAnimo.emoji} ${entry.estadoAnimo.etiqueta}" else ""
                        NotificationHelper.mostrarNotificacionBitacora(
                            context = this@CuidaFamilyMonitorService,
                            autorNombre = entry.autorNombre,
                            patientNombre = patientNombre,
                            estadoAnimoTexto = animoStr,
                            notaTexto = entry.nota
                        )
                    }
                } else {
                    isBitacoraInitialLoad = false
                }

                previousBitacoraEntryIds = newEntryIds
            }
        }

        turnosJob?.cancel()
        turnosJob = serviceScope.launch {
            TurnoRepository.obtenerTodosLosTurnos(familyGroupId).collectLatest { turnosList ->
                val newMap = turnosList.associateBy { it.id }

                if (!isTurnosInitialLoad) {
                    for (turno in turnosList) {
                        val prev = previousTurnosMap[turno.id]
                        val estadoAnterior = prev?.confirmacion ?: EstadoConfirmacion.PENDIENTE
                        val estadoNuevo = turno.confirmacion

                        val diasTexto = turno.diasSemana.map { dayNum ->
                            when (dayNum) {
                                2 -> "L"; 3 -> "M"; 4 -> "X"; 5 -> "J"; 6 -> "V"; 7 -> "S"; 1 -> "D"; else -> ""
                            }
                        }.filter { it.isNotBlank() }.joinToString(", ")
                        val horarioTexto = "$diasTexto · ${turno.horaInicio} - ${turno.horaFin}"

                        if (prev == null) {
                            if (turno.activo && turno.cuidadorUserId == userId) {
                                NotificationHelper.mostrarNotificacionTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    titulo = "📅 Nuevo Turno Asignado",
                                    mensaje = "Se te asignó un nuevo turno ($horarioTexto). Confirma tu disponibilidad en la app."
                                )
                            }
                        } else {
                            // 1. Notificación de Turno Modificado para Cuidador
                            if (turno.activo && turno.cuidadorUserId == userId && prev.fechaUltimaModificacion != turno.fechaUltimaModificacion && turno.fechaUltimaModificacion > 0L) {
                                Log.d("TurnoAviso", "Turno ${turno.id} modificado, notificando a ${turno.cuidadorNombre}")
                                val msgCambio = if (turno.avisoParaCuidador.isNotBlank()) turno.avisoParaCuidador else "Confirma de nuevo si puedes cubrirlo."
                                NotificationHelper.mostrarNotificacionCambioTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    turnoId = turno.id,
                                    titulo = "✏️ Tu turno fue modificado",
                                    mensaje = msgCambio
                                )
                                Log.d("TurnoAviso", "Notificación enviada")
                            }

                            // 2. Notificación de Reasignación HACIA el usuario
                            if (turno.activo && prev.cuidadorUserId != userId && turno.cuidadorUserId == userId) {
                                NotificationHelper.mostrarNotificacionCambioTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    turnoId = turno.id,
                                    titulo = "📅 Se te asignó un turno",
                                    mensaje = "El turno del $horarioTexto te fue asignado. Confirma tu disponibilidad."
                                )
                            }

                            // 3. Notificación de Reasignación DESDE el usuario hacia otro
                            if (prev.cuidadorUserId == userId && turno.cuidadorUserId != userId && turno.activo) {
                                NotificationHelper.mostrarNotificacionCambioTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    turnoId = turno.id,
                                    titulo = "ℹ️ Tu turno fue reasignado",
                                    mensaje = "El turno del $horarioTexto fue reasignado a otro cuidador."
                                )
                            }

                            // 4. Notificación de Cancelación de Turno
                            if (prev.cuidadorUserId == userId && prev.activo && !turno.activo) {
                                NotificationHelper.mostrarNotificacionCambioTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    turnoId = turno.id,
                                    titulo = "🗑️ Tu turno fue cancelado",
                                    mensaje = "El turno del $horarioTexto fue cancelado por el Administrador."
                                )
                            }

                            // 5. Notificación de Respuesta del Cuidador para el Administrador
                            if (estadoAnterior != estadoNuevo && estadoNuevo != EstadoConfirmacion.PENDIENTE && turno.cuidadorUserId != userId) {
                                Log.d("TurnoNotif", "Cambio de ${turno.cuidadorNombre}: $estadoAnterior -> $estadoNuevo, usuario actual=$userId")

                                val (tituloNotif, msgNotif) = when (estadoNuevo) {
                                    EstadoConfirmacion.CONFIRMADO -> Pair(
                                        "✅ ${turno.cuidadorNombre} confirmó su turno",
                                        horarioTexto
                                    )
                                    EstadoConfirmacion.RECHAZADO -> Pair(
                                        "❌ ${turno.cuidadorNombre} no puede cubrir el turno",
                                        "$horarioTexto. Motivo: ${turno.motivoRechazo.ifBlank { "sin motivo" }}"
                                    )
                                    EstadoConfirmacion.REEMPLAZO_SOLICITADO -> Pair(
                                        "🔄 ${turno.cuidadorNombre} solicita reemplazo",
                                        "$horarioTexto. Motivo: ${turno.motivoRechazo.ifBlank { "sin motivo" }}"
                                    )
                                    else -> Pair("📅 Actualización de Turno", horarioTexto)
                                }

                                NotificationHelper.mostrarNotificacionRespuestaTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    turnoId = turno.id,
                                    titulo = tituloNotif,
                                    mensaje = msgNotif
                                )
                            }
                        }
                    }
                } else {
                    isTurnosInitialLoad = false
                }

                previousTurnosMap = newMap
            }
        }

        CalendarRepository.monitorizarEventosDelGrupo(familyGroupId)
        eventosJob?.cancel()
        eventosJob = serviceScope.launch {
            CalendarRepository.allEventsFlow.collectLatest { eventsMap ->
                val eventsList = eventsMap.values.toList()

                if (!isEventosInitialLoad) {
                    for (event in eventsList) {
                        val prev = previousEventosMap[event.id]
                        if (prev == null) {
                            if (event.asignadoAUserId == userId && event.creadoPorUserId != userId) {
                                NotificationHelper.mostrarNotificacionTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    titulo = "📅 Evento Asignado",
                                    mensaje = "Se te asignó: ${event.titulo} el ${event.fecha}. Confirma tu asistencia."
                                )
                            }
                        } else if (prev.confirmacionAsistencia != event.confirmacionAsistencia) {
                            if (event.creadoPorUserId == userId || userId != event.asignadoAUserId) {
                                val estadoStr = when (event.confirmacionAsistencia) {
                                    EstadoConfirmacion.CONFIRMADO -> "confirmó"
                                    EstadoConfirmacion.RECHAZADO -> "no puede asistir a"
                                    EstadoConfirmacion.REEMPLAZO_SOLICITADO -> "solicita reemplazo en"
                                    else -> "actualizó la asistencia de"
                                }
                                val motivoStr = if (event.motivoRechazo.isNotBlank()) " (Motivo: \"${event.motivoRechazo}\")" else ""
                                NotificationHelper.mostrarNotificacionTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    titulo = "📅 Respuesta de Asistencia",
                                    mensaje = "${event.asignadoANombre ?: "Un miembro"} $estadoStr: ${event.titulo}$motivoStr."
                                )
                            }
                        }
                    }
                } else {
                    isEventosInitialLoad = false
                }

                previousEventosMap = eventsMap
            }
        }

        solicitudesJob?.cancel()
        solicitudesJob = serviceScope.launch {
            TurnoRepository.obtenerSolicitudesIntercambio(familyGroupId).collectLatest { lista ->
                val newMap = lista.associateBy { it.id }

                if (!isSolicitudesInitialLoad) {
                    for (sol in lista) {
                        val prev = previousSolicitudesMap[sol.id]
                        if (prev == null) {
                            if (sol.estado == com.example.cuidafamily.model.EstadoSolicitud.PENDIENTE && sol.solicitanteUserId != userId) {
                                val titulo = "🔄 Solicitud de Cambio de Turno"
                                val msg = "🔄 ${sol.solicitanteNombre} solicita cambiar su turno del ${sol.fecha} (${sol.horaInicio} - ${sol.horaFin})"
                                NotificationHelper.mostrarNotificacionCambioTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    turnoId = sol.id,
                                    titulo = titulo,
                                    mensaje = msg
                                )
                            }
                        } else if (prev.estado != sol.estado) {
                            if (sol.solicitanteUserId == userId && !sol.vistaPorSolicitante) {
                                val (titulo, msg) = if (sol.estado == com.example.cuidafamily.model.EstadoSolicitud.APROBADA) {
                                    Pair("✅ Cambio de Turno Aprobado", "✅ Tu cambio para el ${sol.fecha} fue aprobado.")
                                } else {
                                    Pair("❌ Cambio de Turno Rechazado", "❌ Tu solicitud del ${sol.fecha} fue rechazada: ${sol.motivoRechazo.ifBlank { "sin motivo" }}")
                                }
                                NotificationHelper.mostrarNotificacionCambioTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    turnoId = sol.id,
                                    titulo = titulo,
                                    mensaje = msg
                                )
                            }
                        }
                    }
                } else {
                    isSolicitudesInitialLoad = false
                }

                previousSolicitudesMap = newMap
            }
        }

        excepcionesJob?.cancel()
        excepcionesJob = serviceScope.launch {
            TurnoRepository.obtenerExcepcionesTurno(familyGroupId).collectLatest { lista ->
                val newMap = lista.associateBy { it.id }

                if (!isExcepcionesInitialLoad) {
                    for (exc in lista) {
                        val prev = previousExcepcionesMap[exc.id]
                        if (prev == null || prev.confirmacion != exc.confirmacion) {
                            if (exc.cuidadorReemplazoUserId == userId && !exc.vistaPorReemplazo) {
                                val titulo = "📅 Reemplazo de Turno Asignado"
                                val msg = "📅 Se te asignó cubrir el turno del ${exc.fecha} (${exc.cuidadorOriginalNombre})"
                                NotificationHelper.mostrarNotificacionCambioTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    turnoId = exc.id,
                                    titulo = titulo,
                                    mensaje = msg
                                )
                            } else if (exc.confirmacion == EstadoConfirmacion.RECHAZADO && exc.cuidadorOriginalUserId != userId) {
                                val titulo = "❌ Reemplazo Rechazado"
                                val msg = "❌ ${exc.cuidadorReemplazoNombre} no puede cubrir el reemplazo del ${exc.fecha}"
                                NotificationHelper.mostrarNotificacionCambioTurno(
                                    context = this@CuidaFamilyMonitorService,
                                    turnoId = exc.id,
                                    titulo = titulo,
                                    mensaje = msg
                                )
                            }
                        }
                    }
                } else {
                    isExcepcionesInitialLoad = false
                }

                previousExcepcionesMap = newMap
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CuidaFamilyMonitorService destruido. Cancelando listeners.")
        sosJob?.cancel()
        bitacoraJob?.cancel()
        turnosJob?.cancel()
        eventosJob?.cancel()
        solicitudesJob?.cancel()
        excepcionesJob?.cancel()
    }

    companion object {
        private const val TAG = "CuidaFamilyService"
        private const val SERVICE_NOTIFICATION_ID = 1001

        const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        const val EXTRA_USER_NAME = "EXTRA_USER_NAME"
        const val EXTRA_PATIENT_NOMBRE = "EXTRA_PATIENT_NOMBRE"

        fun iniciarServicio(
            context: Context,
            familyGroupId: String,
            userId: String,
            userName: String,
            patientNombre: String = "el adulto mayor"
        ) {
            if (familyGroupId.isBlank() || userId.isBlank()) return

            val intent = Intent(context, CuidaFamilyMonitorService::class.java).apply {
                putExtra(EXTRA_GROUP_ID, familyGroupId)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_PATIENT_NOMBRE, patientNombre)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Solicitando inicio de CuidaFamilyMonitorService para usuario $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error iniciando CuidaFamilyMonitorService", e)
            }
        }

        fun detenerServicio(context: Context) {
            try {
                val intent = Intent(context, CuidaFamilyMonitorService::class.java)
                context.stopService(intent)
                Log.d(TAG, "CuidaFamilyMonitorService detenido")
            } catch (e: Exception) {
                Log.e(TAG, "Error deteniendo CuidaFamilyMonitorService", e)
            }
        }
    }
}

package com.example.cuidafamily.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cuidafamily.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// TODO: migrar a FCM + Cloud Functions para notificación push real entre dispositivos cerrados.

object NotificationHelper {
    const val CHANNEL_ID = "recordatorios_cuidafamily"
    const val CHANNEL_NAME = "Recordatorios de CuidaFamily"

    const val SOS_CHANNEL_ID = "alertas_sos"
    const val SOS_CHANNEL_NAME = "Alertas de Emergencia SOS"

    const val BITACORA_CHANNEL_ID = "bitacora_novedades_v2"
    const val BITACORA_CHANNEL_NAME = "Novedades de la Bitácora"

    const val TURNOS_CHANNEL_ID = "turnos_notificaciones_v1"
    const val TURNOS_CHANNEL_NAME = "Notificaciones de Turnos"

    const val TURNOS_RESPUESTAS_CHANNEL_ID = "turnos_respuestas_v1"
    const val TURNOS_RESPUESTAS_CHANNEL_NAME = "Respuestas de Turnos"

    const val TURNOS_CAMBIOS_CHANNEL_ID = "turnos_cambios_v1"
    const val TURNOS_CAMBIOS_CHANNEL_NAME = "Cambios de Turnos"

    const val SERVICE_CHANNEL_ID = "cuidafamily_service_channel"
    const val SERVICE_CHANNEL_NAME = "Servicio de Monitoreo CuidaFamily"

    fun inicializarCanales(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelRecordatorios = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para recordatorios de citas y tareas médicas compartidas"
            }

            val soundBitacora = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val channelBitacora = NotificationChannel(
                BITACORA_CHANNEL_ID,
                BITACORA_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para publicaciones y novedades compartidas en la bitácora familiar"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setSound(
                    soundBitacora,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val channelTurnos = NotificationChannel(
                TURNOS_CHANNEL_ID,
                TURNOS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para asignación y confirmación de turnos de cuidado"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setSound(
                    soundBitacora,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val channelTurnosRespuestas = NotificationChannel(
                TURNOS_RESPUESTAS_CHANNEL_ID,
                TURNOS_RESPUESTAS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal de notificaciones para respuestas de turnos de cuidadores"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setSound(
                    soundBitacora,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val channelTurnosCambios = NotificationChannel(
                TURNOS_CAMBIOS_CHANNEL_ID,
                TURNOS_CAMBIOS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal de notificaciones para modificaciones y reasignaciones de turnos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setSound(
                    soundBitacora,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val alarmUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val channelSos = NotificationChannel(
                SOS_CHANNEL_ID,
                SOS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal de máxima prioridad para alertas de emergencia y pánico SOS"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setSound(
                    alarmUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val channelService = NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal persistente de monitoreo en segundo plano"
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(channelRecordatorios)
            notificationManager.createNotificationChannel(channelBitacora)
            notificationManager.createNotificationChannel(channelTurnos)
            notificationManager.createNotificationChannel(channelTurnosRespuestas)
            notificationManager.createNotificationChannel(channelTurnosCambios)
            notificationManager.createNotificationChannel(channelSos)
            notificationManager.createNotificationChannel(channelService)

            Log.d("NotificationHelper", "Todos los canales de notificación fueron creados correctamente")
        }
    }

    fun mostrarNotificacionBitacora(
        context: Context,
        autorNombre: String,
        patientNombre: String,
        estadoAnimoTexto: String = "",
        notaTexto: String = ""
    ) {
        try {
            inicializarCanales(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val messageText = when {
                estadoAnimoTexto.isNotBlank() && notaTexto.isNotBlank() ->
                    "$autorNombre: $estadoAnimoTexto - \"$notaTexto\""
                estadoAnimoTexto.isNotBlank() ->
                    "$autorNombre publicó una novedad ($estadoAnimoTexto) sobre $patientNombre"
                notaTexto.isNotBlank() ->
                    "$autorNombre publicó sobre $patientNombre: \"$notaTexto\""
                else ->
                    "$autorNombre agregó una actualización sobre $patientNombre"
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAVIGATE_TO", "bitacora")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                (System.currentTimeMillis() % 10000).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, BITACORA_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("💬 Nueva novedad sobre $patientNombre")
                .setContentText(messageText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 250, 100, 250))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
            Log.d("NotificationHelper", "Notificación de Bitácora emitida con éxito: $messageText")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error al mostrar notificación: ${e.message}", e)
        }
    }

    fun mostrarNotificacionTurno(
        context: Context,
        titulo: String,
        mensaje: String
    ) {
        try {
            inicializarCanales(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAVIGATE_TO", "turnos")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                (System.currentTimeMillis() % 10000).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, TURNOS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 250, 100, 250))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
            Log.d("NotificationHelper", "Notificación de Turno emitida con éxito: $titulo - $mensaje")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error al mostrar notificación de turno: ${e.message}", e)
        }
    }

    fun mostrarNotificacionRespuestaTurno(
        context: Context,
        turnoId: String,
        titulo: String,
        mensaje: String
    ) {
        try {
            inicializarCanales(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAVIGATE_TO", "turnos")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                turnoId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, TURNOS_RESPUESTAS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 250, 100, 250))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(turnoId.hashCode(), notification)
            Log.d("NotificationHelper", "Notificación de respuesta de turno emitida para $turnoId: $titulo - $mensaje")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error al mostrar notificación de respuesta de turno: ${e.message}", e)
        }
    }

    fun mostrarNotificacionCambioTurno(
        context: Context,
        turnoId: String,
        titulo: String,
        mensaje: String
    ) {
        try {
            inicializarCanales(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("NAVIGATE_TO", "turnos")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                turnoId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, TURNOS_CAMBIOS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 250, 100, 250))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(turnoId.hashCode(), notification)
            Log.d("NotificationHelper", "Notificación de cambio de turno emitida para $turnoId: $titulo - $mensaje")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error al mostrar notificación de cambio de turno: ${e.message}", e)
        }
    }

    fun mostrarNotificacionSos(context: Context, alertId: String, nombreUsuario: String, fecha: String) {
        try {
            inicializarCanales(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("SHOW_SOS_ALERT", true)
                putExtra("ALERT_ID", alertId)
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                alertId.hashCode(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, SOS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("¡ALERTA DE EMERGENCIA SOS!")
                .setContentText("$nombreUsuario necesita ayuda urgente ($fecha)")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(alarmUri)
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
                .setLights(android.graphics.Color.RED, 1000, 500)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(alertId.hashCode(), notification)
            Log.d("NotificationHelper", "Notificación SOS emitida con éxito para $nombreUsuario")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error al mostrar notificación: ${e.message}", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun dispararVibracionSos(context: Context) {
        try {
            val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun programarRecordatorioEvento(context: Context, eventId: String, titulo: String, fecha: String, horaInicio: String) {
        cancelarRecordatorioEvento(context, eventId)

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val fechaEvento: Date = sdf.parse("$fecha $horaInicio") ?: return

            val tiempoEventoMs = fechaEvento.time
            val tiempoRecordatorioMs = tiempoEventoMs - TimeUnit.MINUTES.toMillis(15)
            val tiempoActualMs = System.currentTimeMillis()

            val delayMs = tiempoRecordatorioMs - tiempoActualMs

            if (delayMs <= 0) return

            val inputData = Data.Builder()
                .putString("eventId", eventId)
                .putString("titulo", titulo)
                .putString("hora", horaInicio)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(inputData)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag(eventId)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                eventId,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelarRecordatorioEvento(context: Context, eventId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(eventId)
        WorkManager.getInstance(context).cancelAllWorkByTag(eventId)
    }
}

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val titulo = inputData.getString("titulo") ?: "Evento asignado"
        val hora = inputData.getString("hora") ?: ""
        val eventId = inputData.getString("eventId") ?: "0"

        NotificationHelper.inicializarCanales(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Recordatorio de Actividad")
            .setContentText("$titulo - En 15 minutos ($hora)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(eventId.hashCode(), notification)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error al mostrar notificación: ${e.message}", e)
        }

        return Result.success()
    }
}

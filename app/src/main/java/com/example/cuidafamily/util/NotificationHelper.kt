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

    fun crearCanalNotificacion(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Canal para recordatorios de citas y tareas médicas compartidas"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Crea o actualiza el canal de alta prioridad "alertas_sos" con sonido de alarma y vibración larga.
     */
    fun crearCanalSos(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH

            val alarmUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(SOS_CHANNEL_ID, SOS_CHANNEL_NAME, importance).apply {
                description = "Canal de máxima prioridad para alertas de emergencia y pánico SOS"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setSound(alarmUri, audioAttributes)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Emite una notificación de máxima prioridad con setFullScreenIntent para pantalla bloqueada.
     */
    fun mostrarNotificacionSos(context: Context, alertId: String, nombreUsuario: String, fecha: String) {
        crearCanalSos(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        // PendingIntent para activar la MainActivity directo en pantalla completa (incluso con celular bloqueado)
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

        NotificationHelper.crearCanalNotificacion(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Recordatorio de Actividad")
            .setContentText("$titulo - En 15 minutos ($hora)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(eventId.hashCode(), notification)

        return Result.success()
    }
}

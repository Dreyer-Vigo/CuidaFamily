package com.example.cuidafamily.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cuidafamily.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_ID = "recordatorios_cuidafamily"
    const val CHANNEL_NAME = "Recordatorios de CuidaFamily"

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

    fun programarRecordatorioEvento(context: Context, eventId: String, titulo: String, fecha: String, horaInicio: String) {
        // Cancelamos cualquier recordatorio previo asociado a este id único
        cancelarRecordatorioEvento(context, eventId)

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val fechaEvento: Date = sdf.parse("$fecha $horaInicio") ?: return

            val tiempoEventoMs = fechaEvento.time
            val tiempoRecordatorioMs = tiempoEventoMs - TimeUnit.MINUTES.toMillis(15)
            val tiempoActualMs = System.currentTimeMillis()

            val delayMs = tiempoRecordatorioMs - tiempoActualMs

            // Si el evento es en menos de 15 minutos o ya pasó, no programamos u optamos por enviarlo inmediatamente si es muy reciente.
            if (delayMs <= 0) return

            val inputData = Data.Builder()
                .putString("eventId", eventId)
                .putString("titulo", titulo)
                .putString("hora", horaInicio)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(inputData)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag(eventId) // Se usa el eventId como tag único para cancelación quirúrgica
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
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Reutilizamos un ícono nativo seguro del SDK
            .setContentTitle("Recordatorio de Actividad")
            .setContentText("$titulo - En 15 minutos ($hora)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(eventId.hashCode(), notification)

        return Result.success()
    }
}

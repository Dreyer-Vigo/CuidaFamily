package com.example.cuidafamily.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cuidafamily.MainActivity
import com.example.cuidafamily.model.EstadoToma
import com.example.cuidafamily.model.Medicamento
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.TomaMedicacion
import com.example.cuidafamily.repository.MedicacionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit

data class DiagnosticInfo(
    val notifPermissionGranted: Boolean = false,
    val appNotificationsEnabled: Boolean = false,
    val channelCreated: Boolean = false,
    val channelImportanceHigh: Boolean = false,
    val channelSilenced: Boolean = false,
    val channelPreviaCreated: Boolean = false,
    val channelPreviaImportanceHigh: Boolean = false,
    val channelPreviaSilenced: Boolean = false,
    val exactAlarmsGranted: Boolean = false,
    val demoModeActive: Boolean = true,
    val personalRemindersEnabled: Boolean = true,
    val avisoPrevioActive: Boolean = true,
    val anticipacionMinutos: Int = 5,
    val scheduledAlarmsCount: Int = 0,
    val nextAlarmTime: String? = null,
    val receiverRegistered: Boolean = false,
    val inexactAlarmsCount: Int = 0,
    val discardedPastCount: Int = 0,
    val discardedRecipientCount: Int = 0
)

object RecordatorioScheduler {

    const val CHANNEL_ID = "medicacion_recordatorios"
    const val CHANNEL_ALARMA_PREVIA_ID = "medicacion_alarma_previa"

    const val ACTION_YA_LA_DI = "com.example.cuidafamily.ACTION_YA_LA_DI"
    const val ACTION_ENTENDIDO = "com.example.cuidafamily.ACTION_ENTENDIDO"
    const val ACTION_ALARM = "com.example.cuidafamily.ACTION_ALARM"
    const val ACTION_ALARM_30 = "com.example.cuidafamily.ACTION_ALARM_30"

    fun crearCanalMedicacion(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Medicación",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para avisos y alarmas de tomas de medicación"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun crearCanalAlarmaPrevia(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val channel = NotificationChannel(
                CHANNEL_ALARMA_PREVIA_ID,
                "Aviso Previo de Medicación (Alarma)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal con sonido de alarma persistente para aviso previo de dosis"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setSound(
                    alarmSound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                try {
                    setBypassDnd(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun isDemoModeActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("modo_demo_activo", true)
    }

    fun setDemoModeActive(context: Context, activo: Boolean) {
        val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("modo_demo_activo", activo).apply()
        if (!activo) {
            cancelarTodasLasAlarmas(context)
        }
    }

    fun isPersonalRemindersEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("recibir_recordatorios_personal", true)
    }

    fun setPersonalRemindersEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("recibir_recordatorios_personal", enabled).apply()
        if (!enabled) {
            cancelarTodasLasAlarmas(context)
        }
    }

    fun isAvisoPrevioActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("aviso_previo_activo", true)
    }

    fun setAvisoPrevioActive(context: Context, active: Boolean) {
        val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("aviso_previo_activo", active).apply()
    }

    fun getAnticipacionMinutos(context: Context): Int {
        val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("anticipacion_minutos", 5)
    }

    fun setAnticipacionMinutos(context: Context, minutos: Int) {
        val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("anticipacion_minutos", minutos).apply()
    }

    fun checkReceiverRegistered(context: Context): Boolean {
        return try {
            val componentName = ComponentName(context, MedicacionAlarmReceiver::class.java)
            val info = context.packageManager.getReceiverInfo(componentName, 0)
            info != null
        } catch (e: Exception) {
            false
        }
    }

    fun obtenerEstadoDiagnostico(context: Context): DiagnosticInfo {
        crearCanalMedicacion(context)
        crearCanalAlarmaPrevia(context)

        val notifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val appNotifEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.getNotificationChannel(CHANNEL_ID)
        } else null

        val channelCreated = channel != null
        val channelImportanceHigh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel != null && channel.importance >= NotificationManager.IMPORTANCE_HIGH
        } else {
            true
        }
        val channelSilenced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE
        } else {
            false
        }

        val channelPrevia = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.getNotificationChannel(CHANNEL_ALARMA_PREVIA_ID)
        } else null

        val channelPreviaCreated = channelPrevia != null
        val channelPreviaImportanceHigh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelPrevia != null && channelPrevia.importance >= NotificationManager.IMPORTANCE_HIGH
        } else {
            true
        }
        val channelPreviaSilenced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelPrevia != null && channelPrevia.importance == NotificationManager.IMPORTANCE_NONE
        } else {
            false
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val exactGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }

        val demoActive = isDemoModeActive(context)
        val personalEnabled = isPersonalRemindersEnabled(context)
        val avisoPrevioActivo = isAvisoPrevioActive(context)
        val anticipacionMin = getAnticipacionMinutos(context)
        val receiverReg = checkReceiverRegistered(context)

        val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("last_scheduled_count", 0)
        val nextTime = prefs.getString("last_next_alarm", null)
        val inexact = prefs.getInt("last_inexact_count", 0)
        val descPasado = prefs.getInt("last_discarded_past", 0)
        val descOtro = prefs.getInt("last_discarded_recipient", 0)

        val diag = DiagnosticInfo(
            notifPermissionGranted = notifPerm,
            appNotificationsEnabled = appNotifEnabled,
            channelCreated = channelCreated,
            channelImportanceHigh = channelImportanceHigh,
            channelSilenced = channelSilenced,
            channelPreviaCreated = channelPreviaCreated,
            channelPreviaImportanceHigh = channelPreviaImportanceHigh,
            channelPreviaSilenced = channelPreviaSilenced,
            exactAlarmsGranted = exactGranted,
            demoModeActive = demoActive,
            personalRemindersEnabled = personalEnabled,
            avisoPrevioActive = avisoPrevioActivo,
            anticipacionMinutos = anticipacionMin,
            scheduledAlarmsCount = count,
            nextAlarmTime = nextTime,
            receiverRegistered = receiverReg,
            inexactAlarmsCount = inexact,
            discardedPastCount = descPasado,
            discardedRecipientCount = descOtro
        )

        val a = notifPerm && appNotifEnabled
        val b = channelCreated && !channelSilenced && channelPreviaCreated && !channelPreviaSilenced
        val c = exactGranted
        val d = demoActive
        val n = count
        Log.d("Medicacion", "Estado: notif=$a canal=$b exactas=$c demo=$d alarmas=$n")

        return diag
    }

    fun cancelarNotificacionesYAlarmasDosis(context: Context, deterministaId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val reqPrevio = deterministaId.hashCode() + 100000
        val reqEnHora = deterministaId.hashCode()
        val req30 = deterministaId.hashCode() + 200000

        notificationManager?.cancel(reqPrevio)
        notificationManager?.cancel(reqEnHora)
        notificationManager?.cancel(req30)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MedicacionAlarmReceiver::class.java)

        val p1 = PendingIntent.getBroadcast(context, reqPrevio, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val p2 = PendingIntent.getBroadcast(context, reqEnHora, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val p3 = PendingIntent.getBroadcast(context, req30, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.cancel(p1)
        alarmManager.cancel(p2)
        alarmManager.cancel(p3)
        p1.cancel()
        p2.cancel()
        p3.cancel()
        Log.d("Medicacion", "Alarmas y notificaciones canceladas para dosis $deterministaId")
    }

    fun probarNotificacionAhora(context: Context) {
        crearCanalMedicacion(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "medicacion")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            888888,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val horaActual = LocalTime.now(ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        val notificationText = "500 mg · $horaActual · Prueba de notificación inmediata"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("💊 Hora de Paracetamol (Prueba Ahora)")
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(888888, notification)
            Log.d("Medicacion", "Notificación de prueba enviada inmediatamente")
        } catch (e: SecurityException) {
            Log.e("Medicacion", "SecurityException al probar notificación ahora: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("Medicacion", "Error al probar notificación ahora: ${e.message}", e)
        }
    }

    fun programarPrueba10Segundos(context: Context) {
        crearCanalMedicacion(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val testMs = System.currentTimeMillis() + 10000L

        val intent = Intent(context, MedicacionAlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra("TIPO", "EN_HORA")
            putExtra("MEDICAMENTO_ID", "test_id")
            putExtra("MEDICAMENTO_NOMBRE", "Paracetamol (Prueba 10s)")
            putExtra("DOSIS", "500 mg · 1 tableta")
            putExtra("FECHA", LocalDate.now(ZoneId.systemDefault()).toString())
            putExtra("HORA", LocalTime.now(ZoneId.systemDefault()).plusSeconds(10).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
            putExtra("FAMILY_GROUP_ID", "MOCK_GROUP_ID")
            putExtra("DETERMINISTA_ID", "test_determinista_id")
            putExtra("DESTINATARIO_NOMBRE", "Prueba de Recordatorio")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testMs, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testMs, pendingIntent)
            }
            Log.d("Medicacion", "Alarma de prueba programada para en 10 segundos")
        } catch (e: SecurityException) {
            Log.e("Medicacion", "SecurityException programando alarma de prueba, fallback inexacta: ${e.message}")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testMs, pendingIntent)
        } catch (e: Exception) {
            Log.e("Medicacion", "Error programando alarma de prueba: ${e.message}")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testMs, pendingIntent)
        }
    }

    fun programarPruebaAvisoPrevio10Segundos(context: Context) {
        crearCanalAlarmaPrevia(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val testMs = System.currentTimeMillis() + 10000L
        val anticipacionMin = getAnticipacionMinutos(context)

        val intent = Intent(context, MedicacionAlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra("TIPO", "PREVIO")
            putExtra("MEDICAMENTO_ID", "test_previo_id")
            putExtra("MEDICAMENTO_NOMBRE", "Paracetamol (Prueba Previo)")
            putExtra("DOSIS", "500 mg · 1 tableta")
            putExtra("FECHA", LocalDate.now(ZoneId.systemDefault()).toString())
            putExtra("HORA", LocalTime.now(ZoneId.systemDefault()).plusMinutes(anticipacionMin.toLong()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
            putExtra("FAMILY_GROUP_ID", "MOCK_GROUP_ID")
            putExtra("DETERMINISTA_ID", "test_previo_determinista_id")
            putExtra("DESTINATARIO_NOMBRE", "Prueba de Aviso Previo")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999998,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testMs, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testMs, pendingIntent)
            }
            Log.d("Medicacion", "Aviso previo de prueba programado para en 10 segundos")
        } catch (e: SecurityException) {
            Log.e("Medicacion", "SecurityException programando aviso previo de prueba: ${e.message}")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testMs, pendingIntent)
        } catch (e: Exception) {
            Log.e("Medicacion", "Error programando aviso previo de prueba: ${e.message}")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testMs, pendingIntent)
        }
    }

    fun programarAlarmasProximas48H(context: Context, familyGroupId: String, userId: String, userRole: Role) {
        crearCanalMedicacion(context)
        crearCanalAlarmaPrevia(context)

        if (!isPersonalRemindersEnabled(context)) {
            Log.d("Medicacion", "Interruptor personal 'Recibir recordatorios' desactivado. No se programan alarmas.")
            val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putInt("last_scheduled_count", 0)
                .putString("last_next_alarm", null)
                .putInt("last_inexact_count", 0)
                .putInt("last_discarded_past", 0)
                .putInt("last_discarded_recipient", 0)
                .apply()
            obtenerEstadoDiagnostico(context)
            return
        }

        if (familyGroupId.isBlank() || familyGroupId == "MOCK_GROUP_ID") {
            obtenerEstadoDiagnostico(context)
            return
        }

        val modoDemo = isDemoModeActive(context)
        val avisoPrevioActivo = isAvisoPrevioActive(context)
        val anticipacionMinutos = getAnticipacionMinutos(context)
        val anticipacionMs = TimeUnit.MINUTES.toMillis(anticipacionMinutos.toLong())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()

                val medsSnapshot = db.collection("familyGroups")
                    .document(familyGroupId)
                    .collection("medicamentos")
                    .get()
                    .await()

                val medicamentos = medsSnapshot.documents.mapNotNull { doc ->
                    try {
                        val horariosRaw = doc.get("horarios") as? List<*>
                        val horariosList = horariosRaw?.mapNotNull { it?.toString()?.trim() } ?: emptyList()
                        val diasRaw = doc.get("diasSemana") as? List<*>
                        val diasList = diasRaw?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
                        Medicamento(
                            id = doc.id,
                            familyGroupId = familyGroupId,
                            nombre = doc.getString("nombre") ?: "",
                            dosis = doc.getString("dosis") ?: "",
                            horarios = horariosList,
                            diasSemana = diasList,
                            fechaInicio = doc.getString("fechaInicio") ?: "",
                            fechaFin = doc.getString("fechaFin")?.takeIf { it.isNotBlank() },
                            activo = doc.getBoolean("activo") ?: true
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.filter { it.activo }

                val turnosSnapshot = db.collection("familyGroups")
                    .document(familyGroupId)
                    .collection("turnos")
                    .get()
                    .await()

                val turnos = turnosSnapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.cuidafamily.model.Turno::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }.filter { it.activo }

                val excepcionesSnapshot = try {
                    db.collection("familyGroups")
                        .document(familyGroupId)
                        .collection("turnoExcepciones")
                        .get()
                        .await()
                } catch (e: Exception) {
                    null
                }

                val excepciones = excepcionesSnapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(com.example.cuidafamily.model.ExcepcionTurno::class.java)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                val groupDoc = db.collection("familyGroups").document(familyGroupId).get().await()
                val adminId = groupDoc.getString("adminId") ?: ""

                var adminNombre = "Administrador Familiar"
                if (adminId.isNotBlank()) {
                    val adminDoc = db.collection("users").document(adminId).get().await()
                    adminNombre = adminDoc.getString("nombre") ?: "Administrador Familiar"
                }

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@launch
                val nowMs = System.currentTimeMillis()
                val limitMs = nowMs + TimeUnit.HOURS.toMillis(48)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                val timeSdf = SimpleDateFormat("HH:mm", Locale.US)

                var alarmasProgramadasCount = 0
                var inexactAlarmsCount = 0
                var discardedPastCount = 0
                var discardedRecipientCount = 0
                var proximaHora: String? = null
                val calendar = Calendar.getInstance(TimeZone.getDefault())

                for (dayOffset in 0..1) {
                    calendar.timeInMillis = nowMs
                    calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
                    val fechaStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
                    val localDate = LocalDate.parse(fechaStr)
                    val dayOfWeekVal = calendar.get(Calendar.DAY_OF_WEEK)

                    for (med in medicamentos) {
                        if (med.fechaInicio.isNotBlank() && fechaStr < med.fechaInicio) continue
                        if (med.fechaFin != null && fechaStr > med.fechaFin) continue
                        if (med.diasSemana.isNotEmpty() && !med.diasSemana.contains(dayOfWeekVal)) continue

                        for (horario in med.horarios) {
                            val dosisDate = sdf.parse("$fechaStr $horario") ?: continue
                            val dosisMs = dosisDate.time
                            val previoMs = dosisMs - anticipacionMs
                            val dosis30Ms = dosisMs + TimeUnit.MINUTES.toMillis(30)

                            if (dosisMs <= nowMs && dosis30Ms <= nowMs) {
                                discardedPastCount++
                                continue
                            }

                            val turnoEncontrado = turnos.firstOrNull { turno ->
                                turno.aplicaEnFecha(localDate) && cubreHorario(turno.horaInicio, turno.horaFin, horario)
                            }

                            val uidDestino: String
                            val destinatarioNombre: String

                            if (turnoEncontrado != null && turnoEncontrado.cuidadorUserId.isNotBlank()) {
                                val resultCuidador = com.example.cuidafamily.model.cuidadorEnFecha(
                                    turnoEncontrado,
                                    localDate,
                                    excepciones
                                )
                                uidDestino = resultCuidador.userId
                                destinatarioNombre = resultCuidador.nombre.ifBlank { "Cuidador en turno" }
                            } else {
                                uidDestino = adminId.ifBlank { userId }
                                destinatarioNombre = adminNombre
                            }

                            val esParaMi = modoDemo || (userId.isNotBlank() && uidDestino == userId)

                            if (!esParaMi) {
                                discardedRecipientCount++
                                continue
                            }

                            val horaPrevioStr = if (avisoPrevioActivo && previoMs > nowMs) {
                                timeSdf.format(Date(previoMs))
                            } else {
                                "omitido"
                            }
                            val hora30Str = timeSdf.format(Date(dosis30Ms))

                            Log.d("Medicacion", "Dosis ${med.nombre} ${horario}: previo=${horaPrevioStr}, enHora=${horario}, sinConfirmar=${hora30Str}")

                            if (proximaHora == null && dosisMs > nowMs) proximaHora = "$fechaStr $horario"

                            val yyyyMMdd = fechaStr.replace("-", "")
                            val hhmm = horario.replace(":", "")
                            val deterministaId = "${med.id}_${yyyyMMdd}_$hhmm"

                            // 1. AVISO PREVIO (N minutos antes)
                            if (avisoPrevioActivo && previoMs > nowMs && previoMs <= limitMs) {
                                val reqPrevio = deterministaId.hashCode() + 100000
                                val intentPrevio = Intent(context, MedicacionAlarmReceiver::class.java).apply {
                                    action = ACTION_ALARM
                                    putExtra("TIPO", "PREVIO")
                                    putExtra("MEDICAMENTO_ID", med.id)
                                    putExtra("MEDICAMENTO_NOMBRE", med.nombre)
                                    putExtra("DOSIS", med.dosis)
                                    putExtra("FECHA", fechaStr)
                                    putExtra("HORA", horario)
                                    putExtra("FAMILY_GROUP_ID", familyGroupId)
                                    putExtra("DETERMINISTA_ID", deterministaId)
                                    putExtra("DESTINATARIO_NOMBRE", destinatarioNombre)
                                }
                                val pendingIntentPrevio = PendingIntent.getBroadcast(
                                    context,
                                    reqPrevio,
                                    intentPrevio,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, previoMs, pendingIntentPrevio)
                                    } else {
                                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, previoMs, pendingIntentPrevio)
                                        inexactAlarmsCount++
                                    }
                                    alarmasProgramadasCount++
                                } catch (e: Exception) {
                                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, previoMs, pendingIntentPrevio)
                                    inexactAlarmsCount++
                                    alarmasProgramadasCount++
                                }
                            }

                            // 2. EN HORA
                            if (dosisMs > nowMs && dosisMs <= limitMs) {
                                val reqEnHora = deterministaId.hashCode()
                                val intentEnHora = Intent(context, MedicacionAlarmReceiver::class.java).apply {
                                    action = ACTION_ALARM
                                    putExtra("TIPO", "EN_HORA")
                                    putExtra("MEDICAMENTO_ID", med.id)
                                    putExtra("MEDICAMENTO_NOMBRE", med.nombre)
                                    putExtra("DOSIS", med.dosis)
                                    putExtra("FECHA", fechaStr)
                                    putExtra("HORA", horario)
                                    putExtra("FAMILY_GROUP_ID", familyGroupId)
                                    putExtra("DETERMINISTA_ID", deterministaId)
                                    putExtra("DESTINATARIO_NOMBRE", destinatarioNombre)
                                }
                                val pendingIntentEnHora = PendingIntent.getBroadcast(
                                    context,
                                    reqEnHora,
                                    intentEnHora,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dosisMs, pendingIntentEnHora)
                                    } else {
                                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dosisMs, pendingIntentEnHora)
                                        inexactAlarmsCount++
                                    }
                                    alarmasProgramadasCount++
                                } catch (e: Exception) {
                                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dosisMs, pendingIntentEnHora)
                                    inexactAlarmsCount++
                                    alarmasProgramadasCount++
                                }
                            }

                            // 3. SIN CONFIRMAR (30 minutos después)
                            if (dosis30Ms > nowMs && dosis30Ms <= limitMs) {
                                val req30 = deterministaId.hashCode() + 200000
                                val intent30 = Intent(context, MedicacionAlarmReceiver::class.java).apply {
                                    action = ACTION_ALARM_30
                                    putExtra("TIPO", "SIN_CONFIRMAR")
                                    putExtra("MEDICAMENTO_ID", med.id)
                                    putExtra("MEDICAMENTO_NOMBRE", med.nombre)
                                    putExtra("DOSIS", med.dosis)
                                    putExtra("FECHA", fechaStr)
                                    putExtra("HORA", horario)
                                    putExtra("FAMILY_GROUP_ID", familyGroupId)
                                    putExtra("DETERMINISTA_ID", deterministaId)
                                    putExtra("DESTINATARIO_NOMBRE", destinatarioNombre)
                                }
                                val pendingIntent30 = PendingIntent.getBroadcast(
                                    context,
                                    req30,
                                    intent30,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                try {
                                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dosis30Ms, pendingIntent30)
                                    alarmasProgramadasCount++
                                } catch (e: Exception) {
                                    Log.e("Medicacion", "Error programando alarma a los 30 min: ${e.message}")
                                }
                            }
                        }
                    }
                }

                val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("last_family_group_id", familyGroupId)
                    .putInt("last_scheduled_count", alarmasProgramadasCount)
                    .putString("last_next_alarm", proximaHora)
                    .putInt("last_inexact_count", inexactAlarmsCount)
                    .putInt("last_discarded_past", discardedPastCount)
                    .putInt("last_discarded_recipient", discardedRecipientCount)
                    .apply()

                obtenerEstadoDiagnostico(context)
            } catch (e: Exception) {
                Log.e("Medicacion", "Error al programar alarmas de medicación: ${e.message}", e)
            }
        }
    }

    private fun cubreHorario(horaInicio: String, horaFin: String, horaDosis: String): Boolean {
        if (horaInicio.isBlank() || horaFin.isBlank() || horaDosis.isBlank()) return true
        return try {
            val ini = LocalTime.parse(horaInicio.padStart(5, '0'))
            val fin = LocalTime.parse(horaFin.padStart(5, '0'))
            val dos = LocalTime.parse(horaDosis.padStart(5, '0'))

            if (ini == fin) return true

            if (ini.isBefore(fin)) {
                !dos.isBefore(ini) && !dos.isAfter(fin)
            } else {
                !dos.isBefore(ini) || !dos.isAfter(fin)
            }
        } catch (e: Exception) {
            true
        }
    }

    fun cancelarTodasLasAlarmas(context: Context) {
        val prefs = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("last_scheduled_count", 0)
            .putString("last_next_alarm", null)
            .putInt("last_inexact_count", 0)
            .putInt("last_discarded_past", 0)
            .putInt("last_discarded_recipient", 0)
            .apply()
        Log.d("Medicacion", "Alarmas programadas: 0 (canceladas)")
    }
}

class MedicacionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val tipo = intent.getStringExtra("TIPO") ?: intent.getStringExtra("tipo") ?: "EN_HORA"
        val medId = intent.getStringExtra("MEDICAMENTO_ID") ?: ""
        val medNombre = intent.getStringExtra("MEDICAMENTO_NOMBRE") ?: "Medicamento"
        val dosis = intent.getStringExtra("DOSIS") ?: ""
        val fecha = intent.getStringExtra("FECHA") ?: ""
        val hora = intent.getStringExtra("HORA") ?: ""
        val familyGroupId = intent.getStringExtra("FAMILY_GROUP_ID") ?: ""
        val deterministaId = intent.getStringExtra("DETERMINISTA_ID") ?: ""
        val destinatarioNombre = intent.getStringExtra("DESTINATARIO_NOMBRE") ?: "Familiar"

        Log.d("Medicacion", "Receiver tipo=$tipo $medNombre $hora")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (action == RecordatorioScheduler.ACTION_ENTENDIDO) {
            val notifId = intent.getIntExtra("NOTIF_ID", deterministaId.hashCode() + 100000)
            notificationManager.cancel(notifId)
            Log.d("Medicacion", "Aviso previo entendido: notificación $notifId cancelada")
            return
        }

        if (action == RecordatorioScheduler.ACTION_YA_LA_DI) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val auth = FirebaseAuth.getInstance()
                    val user = auth.currentUser
                    if (user != null && familyGroupId != "MOCK_GROUP_ID" && familyGroupId.isNotBlank()) {
                        val db = FirebaseFirestore.getInstance()
                        val userDoc = db.collection("users").document(user.uid).get().await()
                        val nombre = userDoc.getString("nombre") ?: "Cuidador"
                        val rolStr = userDoc.getString("role") ?: "CUIDADOR_EXTERNO"

                        val toma = TomaMedicacion(
                            id = deterministaId,
                            familyGroupId = familyGroupId,
                            medicamentoId = medId,
                            medicamentoNombre = medNombre,
                            dosis = dosis,
                            fecha = fecha,
                            horaProgramada = hora,
                            estado = EstadoToma.CONFIRMADA,
                            confirmadoPorUserId = user.uid,
                            confirmadoPorNombre = nombre,
                            confirmadoPorRol = rolStr,
                            horaConfirmacion = System.currentTimeMillis()
                        )

                        MedicacionRepository.confirmarToma(familyGroupId, toma)
                        RecordatorioScheduler.cancelarNotificacionesYAlarmasDosis(context, deterministaId)
                    } else {
                        RecordatorioScheduler.cancelarNotificacionesYAlarmasDosis(context, deterministaId)
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("NAVIGATE_TO", "medicacion")
                        }
                        context.startActivity(openIntent)
                    }
                } catch (e: Exception) {
                    Log.e("Medicacion", "Error al procesar Ya la di: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (familyGroupId != "MOCK_GROUP_ID" && familyGroupId.isNotBlank() && deterministaId.isNotBlank()) {
                    val tomaExiste = withTimeoutOrNull(2000L) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val doc = db.collection("familyGroups")
                                .document(familyGroupId)
                                .collection("tomasMedicacion")
                                .document(deterministaId)
                                .get()
                                .await()

                            if (doc.exists()) {
                                val estadoStr = doc.getString("estado")
                                estadoStr == EstadoToma.CONFIRMADA.name || estadoStr == EstadoToma.OMITIDA.name
                            } else {
                                false
                            }
                        } catch (e: Exception) {
                            false
                        }
                    } ?: false

                    if (tomaExiste) {
                        Log.d("Medicacion", "Toma $deterministaId ya registrada, se descarta notificación $tipo")
                        pendingResult.finish()
                        return@launch
                    }
                }

                RecordatorioScheduler.crearCanalMedicacion(context)
                RecordatorioScheduler.crearCanalAlarmaPrevia(context)

                if (tipo == "PREVIO") {
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("NAVIGATE_TO", "medicacion")
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        deterministaId.hashCode() + 100000,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val entendidoIntent = Intent(context, MedicacionAlarmReceiver::class.java).apply {
                        this.action = RecordatorioScheduler.ACTION_ENTENDIDO
                        putExtra("DETERMINISTA_ID", deterministaId)
                        putExtra("NOTIF_ID", deterministaId.hashCode() + 100000)
                    }
                    val entendidoPendingIntent = PendingIntent.getBroadcast(
                        context,
                        deterministaId.hashCode() + 400000,
                        entendidoIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val actionYaLaDiIntent = Intent(context, MedicacionAlarmReceiver::class.java).apply {
                        this.action = RecordatorioScheduler.ACTION_YA_LA_DI
                        putExtra("MEDICAMENTO_ID", medId)
                        putExtra("MEDICAMENTO_NOMBRE", medNombre)
                        putExtra("DOSIS", dosis)
                        putExtra("FECHA", fecha)
                        putExtra("HORA", hora)
                        putExtra("FAMILY_GROUP_ID", familyGroupId)
                        putExtra("DETERMINISTA_ID", deterministaId)
                        putExtra("DESTINATARIO_NOMBRE", destinatarioNombre)
                    }
                    val yaLaDiPendingIntent = PendingIntent.getBroadcast(
                        context,
                        deterministaId.hashCode() + 500000,
                        actionYaLaDiIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val anticipacionMin = RecordatorioScheduler.getAnticipacionMinutos(context)
                    val title = "⏰ En $anticipacionMin minutos: $medNombre"
                    val text = "$dosis · a las $hora · Prepárala ahora"

                    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                    val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("NAVIGATE_TO", "medicacion")
                    }
                    val fullScreenPendingIntent = PendingIntent.getActivity(
                        context,
                        deterministaId.hashCode() + 600000,
                        fullScreenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val builder = NotificationCompat.Builder(context, RecordatorioScheduler.CHANNEL_ALARMA_PREVIA_ID)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setSound(alarmSound)
                        .setVibrate(longArrayOf(0, 800, 400, 800, 400, 800))
                        .setContentIntent(openPendingIntent)
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Entendido", entendidoPendingIntent)
                        .addAction(android.R.drawable.ic_menu_send, "Ya la di", yaLaDiPendingIntent)
                        .setTimeoutAfter(60_000L)
                        .setFullScreenIntent(fullScreenPendingIntent, true)
                        .setAutoCancel(true)

                    val notification = builder.build()
                    notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT

                    try {
                        notificationManager.notify(deterministaId.hashCode() + 100000, notification)
                        Log.d("Medicacion", "Aviso previo emitido para $medNombre $hora")
                    } catch (e: SecurityException) {
                        Log.e("Medicacion", "SecurityException en aviso previo: ${e.message}", e)
                    } catch (e: Exception) {
                        Log.e("Medicacion", "Error en aviso previo: ${e.message}", e)
                    }

                } else if (tipo == "SIN_CONFIRMAR") {
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("NAVIGATE_TO", "medicacion")
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        deterministaId.hashCode() + 200000,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val notification = NotificationCompat.Builder(context, RecordatorioScheduler.CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setContentTitle("⚠️ Dosis sin confirmar")
                        .setContentText("Han pasado 30 min de $medNombre ($dosis · $hora). Revísala en Medicación.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setContentIntent(openPendingIntent)
                        .setAutoCancel(true)
                        .build()

                    try {
                        notificationManager.notify(deterministaId.hashCode() + 200000, notification)
                    } catch (e: SecurityException) {
                        Log.e("Medicacion", "SecurityException en notificación 30 min: ${e.message}", e)
                    } catch (e: Exception) {
                        Log.e("Medicacion", "Error en notificación 30 min: ${e.message}", e)
                    }

                } else {
                    // EN_HORA
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("NAVIGATE_TO", "medicacion")
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        deterministaId.hashCode(),
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val actionIntent = Intent(context, MedicacionAlarmReceiver::class.java).apply {
                        this.action = RecordatorioScheduler.ACTION_YA_LA_DI
                        putExtra("MEDICAMENTO_ID", medId)
                        putExtra("MEDICAMENTO_NOMBRE", medNombre)
                        putExtra("DOSIS", dosis)
                        putExtra("FECHA", fecha)
                        putExtra("HORA", hora)
                        putExtra("FAMILY_GROUP_ID", familyGroupId)
                        putExtra("DETERMINISTA_ID", deterministaId)
                        putExtra("DESTINATARIO_NOMBRE", destinatarioNombre)
                    }
                    val actionPendingIntent = PendingIntent.getBroadcast(
                        context,
                        deterministaId.hashCode() + 5000,
                        actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val notificationText = "$dosis · $hora · Le corresponde a: $destinatarioNombre"

                    val notification = NotificationCompat.Builder(context, RecordatorioScheduler.CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle("💊 Hora de $medNombre")
                        .setContentText(notificationText)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setSound(soundUri)
                        .setVibrate(longArrayOf(0, 250, 100, 250))
                        .setContentIntent(openPendingIntent)
                        .addAction(android.R.drawable.ic_menu_send, "Ya la di", actionPendingIntent)
                        .setAutoCancel(true)
                        .build()

                    try {
                        notificationManager.notify(deterministaId.hashCode(), notification)
                        Log.d("Medicacion", "Notificación mostrada para $medNombre $hora")
                    } catch (e: SecurityException) {
                        Log.e("Medicacion", "SecurityException al mostrar notificación de medicación: ${e.message}", e)
                    } catch (e: Exception) {
                        Log.e("Medicacion", "Error al mostrar notificación de medicación: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("Medicacion", "Error en AlarmReceiver: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class MedicacionBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("Medicacion", "MedicacionBootReceiver disparado: reprogramando alarmas")
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            val fgId = context.getSharedPreferences("medicacion_demo_prefs", Context.MODE_PRIVATE)
                .getString("last_family_group_id", "") ?: ""
            val userId = user?.uid ?: ""
            RecordatorioScheduler.programarAlarmasProximas48H(
                context,
                fgId,
                userId,
                Role.CUIDADOR_EXTERNO
            )
        }
    }
}

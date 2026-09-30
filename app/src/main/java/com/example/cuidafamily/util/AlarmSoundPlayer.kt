package com.example.cuidafamily.util

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

// TODO: migrar a FCM + Cloud Functions para notificación push real entre dispositivos cerrados.

/**
 * Utilidad para reproducir y detener el sonido de alarma de emergencia SOS en primer plano.
 */
object AlarmSoundPlayer {
    private var ringtone: Ringtone? = null

    /**
     * Inicia la reproducción del sonido de alarma en bucle.
     */
    fun playAlarm(context: Context) {
        try {
            stopAlarm()

            val alarmUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val r = RingtoneManager.getRingtone(context.applicationContext, alarmUri)
            if (r != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    r.isLooping = true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    r.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                r.play()
                ringtone = r
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Detiene la reproducción del sonido de alarma inmediatamente.
     */
    fun stopAlarm() {
        try {
            ringtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            ringtone = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

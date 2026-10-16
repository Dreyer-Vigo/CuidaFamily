package com.example.cuidafamily.model

import java.time.LocalTime



enum class TipoEvento {
    CITA_MEDICA,
    TERAPIA,
    VISITA,
    OTRO;

    companion object {
        fun fromString(value: String?): TipoEvento {
            return try {
                valueOf(value ?: "OTRO")
            } catch (e: Exception) {
                OTRO
            }
        }
    }
}

enum class EstadoConfirmacion {
    PENDIENTE,
    CONFIRMADO,
    RECHAZADO,
    REEMPLAZO_SOLICITADO;

    companion object {
        fun fromString(value: String?): EstadoConfirmacion {
            if (value.isNullOrBlank()) return PENDIENTE
            return try {
                valueOf(value)
            } catch (e: Exception) {
                PENDIENTE
            }
        }
    }
}

data class CalendarEvent(
    val id: String = "",
    val titulo: String = "",
    val tipo: TipoEvento = TipoEvento.OTRO,
    val fecha: String = "",
    val horaInicio: LocalTime = LocalTime.NOON, 
    val horaFin: LocalTime = LocalTime.NOON,
    val asignadoAUserId: String? = null,
    val asignadoANombre: String? = null,
    val asignadoARol: Role? = null,
    val creadoPorUserId: String = "",
    val notas: String? = null,
    val completado: Boolean = false,
    val familyGroupId: String = "",
    val confirmacionAsistencia: EstadoConfirmacion = EstadoConfirmacion.PENDIENTE,
    val motivoRechazo: String = "",
    val lugar: String = "",
    val fotoUrl: String = "",
    val rolAsignado: String = "",
    val fechaLimiteRespuesta: Long? = null
)

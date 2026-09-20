package com.example.cuidafamily.model

import java.time.LocalTime

/**
 * Categorías principales de eventos para clasificar la agenda.
 */
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

/**
 * Representa una actividad programada en la agenda compartida del grupo familiar.
 */
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
    val familyGroupId: String = ""
)

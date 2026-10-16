package com.example.cuidafamily.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class EstadoTurno(val etiqueta: String) {
    PROGRAMADO("Programado"),
    EN_CURSO("En Curso"),
    COMPLETADO("Completado")
}

data class Turno(
    val id: String = "",
    val familyGroupId: String = "",
    val cuidadorUserId: String = "",
    val cuidadorNombre: String = "",
    val cuidadorTelefono: String = "",
    val diasSemana: List<Int> = emptyList(),
    val horaInicio: String = "",
    val horaFin: String = "",
    val tareas: List<String> = emptyList(),
    val activo: Boolean = true,
    val creadoPorUserId: String = "",
    val confirmacion: EstadoConfirmacion = EstadoConfirmacion.PENDIENTE,
    val motivoRechazo: String = "",
    val confirmacionFecha: Long = 0L,
    val respuestaVistaPorAdmin: Boolean = true,
    val fechaInicio: Long = 0L,
    val fechaFin: Long? = null,
    val fechaUltimaModificacion: Long = 0L,
    val avisoParaCuidador: String = ""
) {
    fun aplicaEnFecha(fecha: LocalDate): Boolean {
        if (fechaInicio > 0L) {
            val inicioLocalDate = Instant.ofEpochMilli(fechaInicio)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            if (fecha.isBefore(inicioLocalDate)) return false
        }

        if (fechaFin != null && fechaFin > 0L) {
            val finLocalDate = Instant.ofEpochMilli(fechaFin)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            if (fecha.isAfter(finLocalDate)) return false
        }

        val calendarDayOfWeek = when (fecha.dayOfWeek) {
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            DayOfWeek.TUESDAY -> 3
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 5
            DayOfWeek.FRIDAY -> 6
            DayOfWeek.SATURDAY -> 7
        }

        return diasSemana.contains(calendarDayOfWeek)
    }

    fun aplicaEnFecha(fechaIso: String): Boolean {
        if (fechaIso.isBlank()) return false
        return try {
            val date = LocalDate.parse(fechaIso)
            aplicaEnFecha(date)
        } catch (e: Exception) {
            false
        }
    }

    fun esVencido(): Boolean {
        if (fechaFin == null || fechaFin <= 0L) return false
        val hoyUtc = LocalDate.now(ZoneOffset.UTC)
        val finLocalDate = Instant.ofEpochMilli(fechaFin).atZone(ZoneOffset.UTC).toLocalDate()
        return hoyUtc.isAfter(finLocalDate)
    }
}

data class TurnoCheckIn(
    val id: String = "",
    val turnoId: String = "",
    val cuidadorUserId: String = "",
    val fecha: String = "",
    val horaCheckIn: String = "",
    val ubicacionValida: Boolean = false,
    val latitudCapturada: Double = 0.0,
    val longitudCapturada: Double = 0.0
)

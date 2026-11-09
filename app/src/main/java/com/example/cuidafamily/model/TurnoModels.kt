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

enum class TipoIntercambio {
    CEDER,    // El Administrador elige quién cubre
    PROPONER  // Propone a un cuidador concreto
}

enum class EstadoSolicitud {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    CANCELADA
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
    val longitudCapturada: Double = 0.0,
    val manual: Boolean = false,
    val noCumplido: Boolean = false,
    val registradoPorUserId: String = "",
    val registradoPorNombre: String = ""
)

data class SolicitudIntercambio(
    val id: String = "",
    val familyGroupId: String = "",
    val turnoId: String = "",
    val fecha: String = "", // "yyyy-MM-dd"
    val horaInicio: String = "",
    val horaFin: String = "",
    val solicitanteUserId: String = "",
    val solicitanteNombre: String = "",
    val tipo: TipoIntercambio = TipoIntercambio.CEDER,
    val reemplazoPropuestoUserId: String = "",
    val reemplazoPropuestoNombre: String = "",
    val motivo: String = "",
    val estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE,
    val resueltaPorUserId: String = "",
    val resueltaPorNombre: String = "",
    val fechaResolucion: Long = 0L,
    val motivoRechazo: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val vistaPorSolicitante: Boolean = true,
    val vistaPorAdmin: Boolean = false
)

data class ExcepcionTurno(
    val id: String = "", // DETERMINISTA "${turnoId}_${yyyyMMdd}"
    val familyGroupId: String = "",
    val turnoId: String = "",
    val fecha: String = "", // "yyyy-MM-dd"
    val cuidadorOriginalUserId: String = "",
    val cuidadorOriginalNombre: String = "",
    val cuidadorReemplazoUserId: String = "",
    val cuidadorReemplazoNombre: String = "",
    val solicitudId: String = "",
    val confirmacion: EstadoConfirmacion = EstadoConfirmacion.PENDIENTE,
    val vistaPorReemplazo: Boolean = false
)

data class CuidadorEnFechaResult(
    val userId: String,
    val nombre: String,
    val esReemplazo: Boolean = false,
    val excepcion: ExcepcionTurno? = null,
    val tieneSolicitudPendiente: Boolean = false,
    val solicitudPendiente: SolicitudIntercambio? = null
)

fun cuidadorEnFecha(
    turno: Turno,
    fecha: LocalDate,
    excepciones: List<ExcepcionTurno> = emptyList(),
    solicitudes: List<SolicitudIntercambio> = emptyList()
): CuidadorEnFechaResult {
    val fechaIso = fecha.toString()

    val exc = excepciones.firstOrNull {
        it.turnoId == turno.id && it.fecha == fechaIso && it.confirmacion != EstadoConfirmacion.RECHAZADO
    }

    if (exc != null) {
        return CuidadorEnFechaResult(
            userId = exc.cuidadorReemplazoUserId,
            nombre = exc.cuidadorReemplazoNombre,
            esReemplazo = true,
            excepcion = exc
        )
    }

    val solPendiente = solicitudes.firstOrNull {
        it.turnoId == turno.id && it.fecha == fechaIso && it.estado == EstadoSolicitud.PENDIENTE
    }

    return CuidadorEnFechaResult(
        userId = turno.cuidadorUserId,
        nombre = turno.cuidadorNombre,
        esReemplazo = false,
        tieneSolicitudPendiente = solPendiente != null,
        solicitudPendiente = solPendiente
    )
}

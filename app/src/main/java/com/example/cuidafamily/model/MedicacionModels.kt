package com.example.cuidafamily.model

import java.util.Calendar

enum class EstadoToma {
    CONFIRMADA,
    OMITIDA
}

enum class EstadoDosis {
    PENDIENTE,
    CONFIRMADA,
    ATRASADA,
    OMITIDA
}

data class Medicamento(
    val id: String = "",
    val familyGroupId: String = "",
    val nombre: String = "",
    val dosis: String = "", // ej. "50 mg · 1 tableta"
    val horarios: List<String> = emptyList(), // "HH:mm" 24h
    val diasSemana: List<Int> = emptyList(), // Calendar.DAY_OF_WEEK; vacía = todos los días
    val fechaInicio: String = "", // "yyyy-MM-dd"
    val fechaFin: String? = null, // null = sin fin
    val indicaciones: String = "",
    val activo: Boolean = true,
    val creadoPorUserId: String = "",
    val creadoPorNombre: String = ""
)

data class TomaMedicacion(
    val id: String = "", // DETERMINISTA "${medicamentoId}_${yyyyMMdd}_${HHmm}"
    val familyGroupId: String = "",
    val medicamentoId: String = "",
    val medicamentoNombre: String = "",
    val dosis: String = "",
    val fecha: String = "", // "yyyy-MM-dd"
    val horaProgramada: String = "", // "HH:mm"
    val estado: EstadoToma = EstadoToma.CONFIRMADA,
    val confirmadoPorUserId: String = "",
    val confirmadoPorNombre: String = "",
    val confirmadoPorRol: String = "",
    val horaConfirmacion: Long = 0L,
    val nota: String = ""
)

data class DosisItem(
    val medicamentoId: String,
    val medicamentoNombre: String,
    val dosis: String,
    val hora: String, // "HH:mm"
    val fecha: String, // "yyyy-MM-dd"
    val indicaciones: String,
    val estadoCalculado: EstadoDosis,
    val tomaGuardada: TomaMedicacion?,
    val deterministaId: String
)

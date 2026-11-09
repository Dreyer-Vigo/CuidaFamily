package com.example.cuidafamily.model

enum class MetodoPago(val etiqueta: String) {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
    YAPE("Yape"),
    PLIN("Plin"),
    OTRO("Otro");

    companion object {
        fun fromString(value: String?): MetodoPago {
            if (value.isNullOrBlank()) return EFECTIVO
            return try {
                valueOf(value)
            } catch (e: Exception) {
                EFECTIVO
            }
        }
    }
}

/**
 * Modelo de Pago a Cuidador.
 * Requerimiento 16: Requisitos del modelo Pago con valores por defecto en todos los campos.
 */
data class Pago(
    val id: String = "",
    val familyGroupId: String = "",
    val cuidadorUserId: String = "",
    val cuidadorNombre: String = "",
    val monto: Double = 0.0,
    val fecha: String = "", // "yyyy-MM-dd"
    val periodoInicio: String = "", // "yyyy-MM-dd"
    val periodoFin: String = "", // "yyyy-MM-dd"
    val metodo: MetodoPago = MetodoPago.EFECTIVO,
    val referencia: String = "", // nro de operación, opcional
    val nota: String = "",
    val registradoPorUserId: String = "",
    val registradoPorNombre: String = "",
    val fechaRegistro: Long = 0L
)

/**
 * Modelo de Tarifa de Turno.
 * Requerimiento 16: Colección "familyGroups/{groupId}/tarifas/{turnoId}" con tarifaPorHora: Double.
 */
data class TarifaTurno(
    val turnoId: String = "",
    val tarifaPorHora: Double = 0.0
)

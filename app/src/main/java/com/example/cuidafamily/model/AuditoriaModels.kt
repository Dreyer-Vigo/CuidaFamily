package com.example.cuidafamily.model

enum class EntidadAuditoria(val etiqueta: String) {
    FICHA("Ficha Médica"),
    ALERGIA("Alergia"),
    PESO("Peso"),
    CALENDARIO("Calendario"),
    DOCUMENTO("Documento"),
    GASTO("Gasto"),
    PAGO("Pago"),
    TURNO("Turno"),
    INTERCAMBIO("Intercambio de Turno"),
    MEDICACION("Medicación"),
    BITACORA("Bitácora"),
    GRUPO("Grupo Familiar");

    companion object {
        fun fromString(value: String?): EntidadAuditoria {
            if (value.isNullOrBlank()) return FICHA
            return try {
                valueOf(value)
            } catch (e: Exception) {
                FICHA
            }
        }
    }
}

enum class AccionAuditoria(val etiqueta: String, val verboEspanol: String) {
    CREAR("Crear", "creó"),
    MODIFICAR("Modificar", "modificó"),
    ELIMINAR("Eliminar", "eliminó");

    companion object {
        fun fromString(value: String?): AccionAuditoria {
            if (value.isNullOrBlank()) return CREAR
            return try {
                valueOf(value)
            } catch (e: Exception) {
                CREAR
            }
        }
    }
}

/**
 * Requerimiento 07: Modelo RegistroAuditoria.
 * El campo del autor se llama exactamente "userId" para su uso en reglas de seguridad Firestore.
 */
data class RegistroAuditoria(
    val id: String = "",
    val familyGroupId: String = "",
    val entidad: EntidadAuditoria = EntidadAuditoria.FICHA,
    val accion: AccionAuditoria = AccionAuditoria.CREAR,
    val entidadId: String = "",
    val descripcion: String = "",
    val detalleCambios: String = "",
    val userId: String = "",
    val userNombre: String = "",
    val userRol: String = "",
    val fecha: Long = 0L
)

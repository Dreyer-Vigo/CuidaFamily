package com.example.cuidafamily.model

import androidx.compose.ui.graphics.Color

enum class TipoDocumento(
    val etiqueta: String,
    val colorBackgroundHex: Long,
    val colorTextHex: Long
) {
    RECETA("Receta Médica", 0xFFE8F5E9, 0xFF2E7D32),
    EXAMEN_LABORATORIO("Examen de Lab.", 0xFFE3F2FD, 0xFF1565C0),
    RADIOGRAFIA("Radiografía / Imagen", 0xFFEDE7F6, 0xFF673AB7),
    OTRO("Otro Documento", 0xFFFFF3E0, 0xFFE65100);

    val colorBackground: Color get() = Color(colorBackgroundHex)
    val colorText: Color get() = Color(colorTextHex)

    companion object {
        fun fromString(value: String?): TipoDocumento {
            if (value.isNullOrBlank()) return OTRO
            return try {
                valueOf(value)
            } catch (e: Exception) {
                OTRO
            }
        }
    }
}

data class DocumentoMedico(
    val id: String = "",
    val patientId: String = "",
    val familyGroupId: String = "",
    val tipo: TipoDocumento = TipoDocumento.OTRO,
    val nombre: String = "",
    val fechaDocumento: String = "",
    val fechaSubida: String = "",
    val subidoPorUserId: String = "",
    val subidoPorNombre: String = "",
    val uriLocal: String = "",
    val mimeType: String = "",
    val indicaciones: String = "",
    val imagenBase64: String = "",
    val imagenesBase64: List<String> = emptyList(),
    val paginasTotales: Int = 0,
    val paginasCompartidas: Int = 0
) {
    val paginasBase64: List<String>
        get() = if (imagenesBase64.isNotEmpty()) {
            imagenesBase64
        } else if (imagenBase64.isNotBlank()) {
            listOf(imagenBase64)
        } else {
            emptyList()
        }

    val estaCompartidoConGrupo: Boolean
        get() = paginasBase64.isNotEmpty()
}

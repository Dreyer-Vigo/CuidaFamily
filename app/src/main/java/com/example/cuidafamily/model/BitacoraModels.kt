package com.example.cuidafamily.model

import androidx.compose.ui.graphics.Color

enum class EstadoAnimo(
    val emoji: String,
    val etiqueta: String,
    val colorBackgroundHex: Long,
    val colorTextHex: Long
) {
    FELIZ("😊", "Feliz", 0xFFE8F5E9, 0xFF2E7D32),
    TRANQUILO("😌", "Tranquilo", 0xFFE0F7FA, 0xFF00838F),
    NORMAL("😐", "Normal", 0xFFFFF8E1, 0xFFF57F17),
    DECAIDO("😔", "Decaído", 0xFFECEFF1, 0xFF455A64),
    IRRITADO("😠", "Irritado", 0xFFFFEBEE, 0xFFC62828),
    NINGUNO("❓", "Sin especificar", 0xFFF5F5F5, 0xFF616161);

    val colorBackground: Color get() = Color(colorBackgroundHex)
    val colorText: Color get() = Color(colorTextHex)

    companion object {
        fun fromString(value: String?): EstadoAnimo {
            if (value.isNullOrBlank()) return NINGUNO
            return try {
                valueOf(value)
            } catch (e: Exception) {
                NINGUNO
            }
        }
    }
}

data class BitacoraEntrada(
    val id: String = "",
    val familyGroupId: String = "",
    val autorUserId: String = "",
    val autorNombre: String = "",
    val autorRol: Role = Role.COLABORADOR,
    val fecha: String = "",
    val estadoAnimo: EstadoAnimo = EstadoAnimo.NINGUNO,
    val nota: String = "",
    val fotoUrl: String = ""
)

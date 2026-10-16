package com.example.cuidafamily.model

enum class Role {
    ADMINISTRADOR_FAMILIAR,
    COLABORADOR,
    CUIDADOR_EXTERNO;

    companion object {
        fun fromString(value: String?): Role? {
            if (value.isNullOrBlank()) return null
            return try {
                valueOf(value)
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class User(
    val id: String = "",
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val fotoUrl: String? = null,
    val familyGroupId: String = "",
    val role: Role? = null
)

data class FamilyGroup(
    val id: String = "",
    val nombre: String = "",
    val codigoInvitacion: String = "",
    val adminId: String = "",
    val domicilioLat: Double? = null,
    val domicilioLng: Double? = null
)

data class GroupMember(
    val userId: String = "",
    val nombre: String = "",
    val role: Role = Role.COLABORADOR,
    val subtipo: String = "",
    val fechaIngreso: String = ""
)

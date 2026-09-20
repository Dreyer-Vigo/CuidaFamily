package com.example.cuidafamily.model

/**
 * Roles disponibles en el sistema CuidaFamily.
 */
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

/**
 * Representa a un usuario individual en la aplicación.
 * Nota: Se requieren valores por defecto para Firestore.
 */
data class User(
    val id: String = "",
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val fotoUrl: String? = null,
    val familyGroupId: String = "",
    val role: Role? = null
)

/**
 * Representa un núcleo o grupo familiar de cuidado.
 */
data class FamilyGroup(
    val id: String = "",
    val nombre: String = "",
    val codigoInvitacion: String = "",
    val adminId: String = ""
)

/**
 * Vincula a un usuario con un grupo familiar y le asigna un rol específico.
 */
data class GroupMember(
    val userId: String = "",
    val nombre: String = "",
    val role: Role = Role.COLABORADOR,
    val subtipo: String = "", // Para "Hijo/a", "Enfermero/a", "Nieto/a", etc.
    val fechaIngreso: String = ""
)

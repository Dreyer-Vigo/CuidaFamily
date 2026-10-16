package com.example.cuidafamily.model

data class SosAlert(
    val id: String = "",
    val familyGroupId: String = "",
    val disparadoPorUserId: String = "",
    val disparadoPorNombre: String = "",
    val fecha: String = "",
    val atendida: Boolean = false,
    val atendidaPorUserId: String = "",
    val atendidaPorNombre: String = ""
)

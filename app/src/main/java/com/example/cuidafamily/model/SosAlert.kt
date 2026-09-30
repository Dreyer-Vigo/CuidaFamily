package com.example.cuidafamily.model

// TODO: migrar a FCM + Cloud Functions para notificación push real entre dispositivos cerrados.

/**
 * Modelo de datos para las Alertas SOS / Botón de Pánico en tiempo real.
 * Ruta en Firestore: familyGroups/{familyGroupId}/sosAlerts/{alertId}
 */
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

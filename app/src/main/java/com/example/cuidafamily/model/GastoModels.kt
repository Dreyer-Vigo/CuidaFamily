package com.example.cuidafamily.model

data class Gasto(
    val id: String = "",
    val familyGroupId: String = "",
    val concepto: String = "",
    val monto: Double = 0.0,
    val categoria: String = "General",
    val fecha: String = "",
    val comprobanteUrl: String = "",
    val comprobanteBase64: String = "",
    val comprobantesBase64: List<String> = emptyList(),
    val registradoPorUserId: String = "",
    val registradoPorNombre: String = "",
    val fechaRegistro: Long = System.currentTimeMillis()
) {
    val listaComprobantes: List<String>
        get() = if (comprobantesBase64.isNotEmpty()) {
            comprobantesBase64
        } else if (comprobanteBase64.isNotBlank()) {
            listOf(comprobanteBase64)
        } else {
            emptyList()
        }
}

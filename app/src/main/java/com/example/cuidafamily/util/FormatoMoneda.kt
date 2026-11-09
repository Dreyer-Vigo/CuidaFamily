package com.example.cuidafamily.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Requerimiento: Formato único de moneda en Soles Peruanos (PEN).
 * Devuelve "S/ 1,234.50" o "-S/ 50.00" independientemente de la configuración de idioma del dispositivo.
 */
fun formatearSoles(monto: Double): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val decimalFormat = DecimalFormat("#,##0.00", symbols)
    val absMonto = abs(monto)
    val formatted = decimalFormat.format(absMonto)
    return if (monto < 0) "-S/ $formatted" else "S/ $formatted"
}

/**
 * Suma montos de forma precisa usando BigDecimal con redondeo HALF_UP a 2 decimales.
 */
fun sumarMontosPrecision(montos: List<Double>): Double {
    var sum = BigDecimal.ZERO
    for (m in montos) {
        sum = sum.add(BigDecimal.valueOf(m))
    }
    return sum.setScale(2, RoundingMode.HALF_UP).toDouble()
}

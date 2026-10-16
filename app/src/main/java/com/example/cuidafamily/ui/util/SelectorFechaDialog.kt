package com.example.cuidafamily.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cuidafamily.ui.theme.GradientEnd
import com.example.cuidafamily.ui.theme.GradientStart
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.theme.LavandaPrimaryLight
import com.example.cuidafamily.ui.theme.TextoPrincipalColor
import com.example.cuidafamily.ui.theme.TextoSecundarioColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

val DIAS_SEMANA_ESP = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

val NOMBRES_DIAS_COMPLETOS_ESP = listOf(
    "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
)

val NOMBRES_MESES_ESP = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

val NOMBRES_MESES_CORTOS_ESP = listOf(
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic"
)

fun formatFechaEspanolLarga(date: LocalDate): String {
    val diaNombre = NOMBRES_DIAS_COMPLETOS_ESP[date.dayOfWeek.value - 1]
    val mesNombre = NOMBRES_MESES_ESP[date.monthValue - 1].lowercase()
    return "$diaNombre ${date.dayOfMonth} de $mesNombre de ${date.year}"
}

fun formatFechaEspanolCorta(date: LocalDate): String {
    val diaNombre = NOMBRES_DIAS_COMPLETOS_ESP[date.dayOfWeek.value - 1].take(3)
    val mesNombre = NOMBRES_MESES_CORTOS_ESP[date.monthValue - 1]
    return "$diaNombre ${date.dayOfMonth} $mesNombre ${date.year}"
}

@Composable
fun SelectorFechaDialog(
    fechaInicial: LocalDate? = null,
    titulo: String = "Seleccionar fecha",
    fechaMinima: LocalDate? = null,
    onFechaSeleccionada: (LocalDate) -> Unit,
    onCancelar: () -> Unit
) {
    val hoy = remember { LocalDate.now() }
    var fechaSeleccionada by remember { mutableStateOf(fechaInicial ?: hoy) }
    var yearMonthVisible by remember { mutableStateOf(YearMonth.from(fechaSeleccionada)) }

    val gradientBrush = Brush.linearGradient(listOf(GradientStart, GradientEnd))

    Dialog(onDismissRequest = onCancelar) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Encabezado del Diálogo
                Text(
                    text = titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LavandaPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatFechaEspanolLarga(fechaSeleccionada),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoPrincipalColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de Mes con Flechas ‹ ›
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { yearMonthVisible = yearMonthVisible.minusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = LavandaPrimary)
                    }

                    Text(
                        text = "${NOMBRES_MESES_ESP[yearMonthVisible.monthValue - 1]} ${yearMonthVisible.year}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextoPrincipalColor
                    )

                    IconButton(
                        onClick = { yearMonthVisible = yearMonthVisible.plusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = LavandaPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fila de Nombres de Días de la Semana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    DIAS_SEMANA_ESP.forEach { diaNombre ->
                        Text(
                            text = diaNombre,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoSecundarioColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grilla de Días del Mes
                val primerDiaDelMes = yearMonthVisible.atDay(1)
                val primerDiaSemanaValue = primerDiaDelMes.dayOfWeek.value // 1=Lun ... 7=Dom
                val maxDiasDelMes = yearMonthVisible.lengthOfMonth()

                val semanasGrid = mutableListOf<List<LocalDate?>>()
                var semanaActual = mutableListOf<LocalDate?>()

                for (i in 1 until primerDiaSemanaValue) {
                    semanaActual.add(null)
                }

                for (diaNum in 1..maxDiasDelMes) {
                    semanaActual.add(yearMonthVisible.atDay(diaNum))
                    if (semanaActual.size == 7) {
                        semanasGrid.add(semanaActual)
                        semanaActual = mutableListOf()
                    }
                }

                if (semanaActual.isNotEmpty()) {
                    while (semanaActual.size < 7) {
                        semanaActual.add(null)
                    }
                    semanasGrid.add(semanaActual)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    semanasGrid.forEach { semana ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            semana.forEach { dia ->
                                if (dia == null) {
                                    Spacer(modifier = Modifier.width(36.dp))
                                } else {
                                    val isSelected = dia == fechaSeleccionada
                                    val isHoy = dia == hoy
                                    val isDisabled = fechaMinima != null && dia.isBefore(fechaMinima)

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.background(gradientBrush, CircleShape)
                                                } else if (isHoy) {
                                                    Modifier.border(1.5.dp, LavandaPrimary, CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable(enabled = !isDisabled) {
                                                fechaSeleccionada = dia
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dia.dayOfMonth.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected || isHoy) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isDisabled -> Color.LightGray
                                                isSelected -> Color.White
                                                isHoy -> LavandaPrimary
                                                else -> TextoPrincipalColor
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones Cancelar y Aceptar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancelar,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", fontSize = 15.sp, color = TextoSecundarioColor)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { onFechaSeleccionada(fechaSeleccionada) },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aceptar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

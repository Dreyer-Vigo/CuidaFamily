package com.example.cuidafamily.ui.gastos

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.cuidafamily.model.MetodoPago
import com.example.cuidafamily.model.Pago
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.repository.GroupMemberInfo
import com.example.cuidafamily.ui.theme.*
import com.example.cuidafamily.ui.util.*
import com.example.cuidafamily.util.OcurrenciaDetalle
import com.example.cuidafamily.util.ResumenPagosCuidador
import com.example.cuidafamily.util.formatearSoles
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Requerimiento 1: Muestra badge con perfil "👩‍⚕️ Enfermero/a" o "🧑‍🤝‍🧑 Cuidador/a".
 */
fun obtenerPerfilBadgeEtiqueta(subtipo: String?): String {
    val s = subtipo?.trim() ?: ""
    return if (s.contains("Enfermer", ignoreCase = true)) {
        "👩‍⚕️ Enfermero/a"
    } else {
        "🧑‍🤝‍🧑 Cuidador/a"
    }
}

/**
 * Requerimiento 3: Nombre corto de mes y año para la imputación (ej: "Noviembre 2026")
 */
fun obtenerNombreMesYAnio(fechaIso: String): String {
    return try {
        val date = LocalDate.parse(fechaIso)
        val nombreMes = NOMBRES_MESES_ESP[date.monthValue - 1]
        "$nombreMes ${date.year}"
    } catch (e: Exception) {
        fechaIso
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagosTabContent(
    viewModel: PagoViewModel,
    familyGroupId: String,
    currentUserId: String,
    currentUserName: String,
    userRole: Role?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var showPagoDialog by remember { mutableStateOf(false) }
    var showExplicacionDialog by remember { mutableStateOf(false) }
    var pagoToEdit by remember { mutableStateOf<Pago?>(null) }
    var pagoToDelete by remember { mutableStateOf<Pago?>(null) }
    var cuidadorPreseleccionado by remember { mutableStateOf<ResumenPagosCuidador?>(null) }
    var resumenDetalleDialog by remember { mutableStateOf<ResumenPagosCuidador?>(null) }

    val yearMonth = state.selectedYearMonth
    val hayCuidadoresExternos = state.miembrosCuidadores.isNotEmpty()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Card de Selector de Mes y Resumen Total Pagado
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Selector de Mes ‹ Octubre 2026 ›
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setMes(yearMonth.minusMonths(1)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = LavandaPrimary)
                    }

                    val nombreMes = NOMBRES_MESES_ESP[yearMonth.monthValue - 1]
                    Text(
                        text = "‹ $nombreMes ${yearMonth.year} ›",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = { viewModel.setMes(yearMonth.plusMonths(1)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = LavandaPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total Pagado a Cuidadores:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextoSecundarioColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatearSoles(state.totalPagadoMes),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "💡 Cuenta el mes del 'Periodo hasta' del pago",
                            fontSize = 11.sp,
                            color = TextoSecundarioColor
                        )
                    }

                    IconButton(
                        onClick = { exportarPagosCsv(context, state.historialPagosFiltrados.ifEmpty { state.historialPagosMes }) },
                        modifier = Modifier
                            .background(LavandaPrimaryLight, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Exportar CSV", tint = LavandaPrimary)
                    }
                }

                // Botón "Registrar Nuevo Pago" centrado y con estilo destacado de botón principal
                if (hayCuidadoresExternos) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            cuidadorPreseleccionado = null
                            pagoToEdit = null
                            showPagoDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Registrar Nuevo Pago",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interruptor "Contar turnos sin check-in como cumplidos"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contar turnos sin check-in como cumplidos",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextoPrincipalColor,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.contarSinCheckIn,
                        onCheckedChange = { enabled ->
                            viewModel.setContarSinCheckIn(enabled, context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = LavandaPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LavandaPrimary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Sección: Resumen por Cuidador + Ícono de Explicación "i"
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Resumen por Cuidador",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { showExplicacionDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Explicación de cálculo", tint = LavandaPrimary)
                        }
                    }
                }

                // Requerimiento 1: Estado vacío cuando no hay Cuidadores Externos
                if (!hayCuidadoresExternos) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.PeopleOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Aún no hay cuidadores externos en tu grupo. Invítalos con el código de invitación.",
                                    color = TextoPrincipalColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (state.resumenesCuidadores.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No hay datos de turnos para los cuidadores externos en este mes.", color = TextoSecundarioColor, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    items(state.resumenesCuidadores, key = { it.cuidadorUserId }) { resumen ->
                        TarjetaResumenCuidador(
                            resumen = resumen,
                            onCardClick = { resumenDetalleDialog = resumen },
                            onRegistrarPago = {
                                cuidadorPreseleccionado = resumen
                                pagoToEdit = null
                                showPagoDialog = true
                            }
                        )
                    }
                }

                // Requerimiento 2: Sección Historial Independiente del Mes con Filtros
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        Text(
                            text = "Historial de Pagos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Chips de Filtro: Mes (Todos o seleccionado) y Cuidador
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = state.historialFiltroMes == null,
                                    onClick = { viewModel.setHistorialFiltroMes(null) },
                                    label = { Text("Todos los meses", fontSize = 12.sp) }
                                )
                            }
                            item {
                                val nombreMes = NOMBRES_MESES_ESP[yearMonth.monthValue - 1]
                                FilterChip(
                                    selected = state.historialFiltroMes == yearMonth,
                                    onClick = { viewModel.setHistorialFiltroMes(yearMonth) },
                                    label = { Text("Mes $nombreMes ${yearMonth.year}", fontSize = 12.sp) }
                                )
                            }
                            if (state.miembrosCuidadores.size > 1) {
                                item {
                                    FilterChip(
                                        selected = state.historialFiltroCuidadorId.isBlank(),
                                        onClick = { viewModel.setHistorialFiltroCuidadorId("") },
                                        label = { Text("Todos los cuidadores", fontSize = 12.sp) }
                                    )
                                }
                                items(state.miembrosCuidadores, key = { it.member.userId }) { m ->
                                    FilterChip(
                                        selected = state.historialFiltroCuidadorId == m.member.userId,
                                        onClick = { viewModel.setHistorialFiltroCuidadorId(m.member.userId) },
                                        label = { Text(m.userName, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.historialPagosFiltrados.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No hay pagos en el historial para los filtros seleccionados.", color = TextoSecundarioColor, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    items(state.historialPagosFiltrados, key = { it.id }) { pago ->
                        val isHighlighted = pago.id == state.highlightedPagoId
                        TarjetaPagoHistorial(
                            pago = pago,
                            currentUserId = currentUserId,
                            selectedYearMonth = yearMonth,
                            isHighlighted = isHighlighted,
                            onEditar = {
                                pagoToEdit = pago
                                showPagoDialog = true
                            },
                            onEliminar = {
                                pagoToDelete = pago
                            }
                        )
                    }
                }

                // Requerimiento 6: Línea de diagnóstico visible en pantalla
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFECEFF1),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pagos leídos: ${state.totalPagosLeidos} | del mes: ${state.totalPagosMesCount} | cuidadores: ${state.totalCuidadoresCount} | rol: ${userRole?.name ?: "N/A"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF455A64),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showExplicacionDialog) {
        AlertDialog(
            onDismissRequest = { showExplicacionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = LavandaPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¿Cómo se calculan los pagos?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Devengado: Horas de turnos cumplidos × tarifa por hora del turno.", fontSize = 13.sp, color = TextoPrincipalColor)
                    Text("• Pagado: Suma de los pagos cuya fecha 'Periodo hasta' pertenece al mes seleccionado.", fontSize = 13.sp, color = TextoPrincipalColor)
                    Text("• Pendiente: Devengado − Pagado.", fontSize = 13.sp, color = TextoPrincipalColor)
                    Text("• Adelanto: Si el Pago supera el Devengado (o no hay turnos cumplidos aún), el saldo sale como 'Adelanto de S/ X'.", fontSize = 12.sp, color = TextoSecundarioColor, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExplicacionDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showPagoDialog || pagoToEdit != null) {
        FormularioPagoDialog(
            initialPago = pagoToEdit,
            cuidadorPreseleccionado = cuidadorPreseleccionado,
            miembrosCuidadores = state.miembrosCuidadores,
            cuidadoresResumen = state.resumenesCuidadores,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            isSaving = state.isSaving,
            onDismiss = {
                showPagoDialog = false
                pagoToEdit = null
                cuidadorPreseleccionado = null
            },
            onGuardar = { pago ->
                viewModel.crearOEditarPago(pago)
                showPagoDialog = false
                pagoToEdit = null
                cuidadorPreseleccionado = null
            }
        )
    }

    if (pagoToDelete != null) {
        AlertDialog(
            onDismissRequest = { pagoToDelete = null },
            title = { Text("Eliminar pago", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("¿Deseas eliminar el registro de pago de ${formatearSoles(pagoToDelete?.monto ?: 0.0)} a \"${pagoToDelete?.cuidadorNombre}\"?", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        pagoToDelete?.let { viewModel.eliminarPago(it) }
                        pagoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RojoAlerta),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pagoToDelete = null }, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (resumenDetalleDialog != null) {
        DetalleTurnosDialog(
            resumen = resumenDetalleDialog!!,
            onDismiss = { resumenDetalleDialog = null },
            onMarcarCumplida = { ocurrencia ->
                viewModel.marcarCumplimientoOcurrencia(
                    turnoId = ocurrencia.turnoId,
                    fechaIso = ocurrencia.fechaIso,
                    cuidadorUserId = ocurrencia.cuidadorUserId,
                    manual = true,
                    noCumplido = false
                )
            },
            onMarcarNoCumplida = { ocurrencia ->
                viewModel.marcarCumplimientoOcurrencia(
                    turnoId = ocurrencia.turnoId,
                    fechaIso = ocurrencia.fechaIso,
                    cuidadorUserId = ocurrencia.cuidadorUserId,
                    manual = false,
                    noCumplido = true
                )
            }
        )
    }
}

@Composable
fun TarjetaResumenCuidador(
    resumen: ResumenPagosCuidador,
    onCardClick: () -> Unit,
    onRegistrarPago: () -> Unit
) {
    val pendiente = resumen.pendiente
    val esAdelanto = pendiente < 0.0

    val colorPendiente = when {
        esAdelanto -> Color(0xFF1976D2) // Azul para Adelanto
        pendiente > 0.0 -> Color(0xFFE65100) // Ámbar / Naranja si > 0
        else -> Color(0xFF2E7D32) // Verde si 0
    }

    val textoPendiente = when {
        esAdelanto -> "Adelanto de ${formatearSoles(kotlin.math.abs(pendiente))}"
        pendiente > 0.0 -> formatearSoles(pendiente)
        else -> "S/ 0.00"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(nombre = resumen.cuidadorNombre, size = 42.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = resumen.cuidadorNombre,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipalColor
                        )
                        Surface(
                            color = LavandaPrimaryLight,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = obtenerPerfilBadgeEtiqueta(resumen.subtipo),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LavandaPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = LavandaPrimaryLight,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${resumen.turnosCumplidosCount} turnos (${resumen.totalHoras.toInt()}h)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LavandaPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Devengado:", fontSize = 12.sp, color = TextoSecundarioColor)
                    if (resumen.turnosCumplidosCount == 0) {
                        Text("Sin turnos cumplidos este mes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    } else if (resumen.tieneTurnoSinTarifa && resumen.devengado == 0.0) {
                        Text("Sin tarifa definida", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    } else {
                        Text(formatearSoles(resumen.devengado), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor)
                    }
                }

                Column {
                    Text("Pagado:", fontSize = 12.sp, color = TextoSecundarioColor)
                    Text(formatearSoles(resumen.pagado), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("PENDIENTE:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorPendiente)
                    Text(
                        text = textoPendiente,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorPendiente
                    )
                }
            }

            if (resumen.tieneTurnoSinTarifa) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFE65100))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Tiene turnos sin tarifa definida (mostrados como 'Sin tarifa' y no suman al devengado)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Toca para ver detalle de turnos ➔",
                    fontSize = 12.sp,
                    color = LavandaPrimary,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onRegistrarPago,
                    colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Registrar pago", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TarjetaPagoHistorial(
    pago: Pago,
    currentUserId: String,
    selectedYearMonth: YearMonth,
    isHighlighted: Boolean = false,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val realUid = FirebaseAuth.getInstance().currentUser?.uid ?: currentUserId
    val esAutor = (pago.registradoPorUserId.isNotBlank() && pago.registradoPorUserId == realUid)

    val refFecha = pago.periodoFin.ifBlank { pago.fecha }
    val mesImputacionStr = refFecha.take(7) // "yyyy-MM"
    val mesActualStr = selectedYearMonth.toString()
    val caeEnOtroMes = mesImputacionStr.isNotBlank() && mesImputacionStr != mesActualStr

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isHighlighted) Color(0xFFF3E5F5) else Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 4.dp else 2.dp),
        border = BorderStroke(if (isHighlighted) 2.dp else 1.dp, if (isHighlighted) LavandaPrimary else LavandaPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pago.cuidadorNombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipalColor
                    )

                    if (caeEnOtroMes) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFE1F5FE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Cuenta en ${obtenerNombreMesYAnio(refFecha)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0288D1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = formatearSoles(pago.monto),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = LavandaPrimaryLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = pago.metodo.etiqueta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LavandaPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Fecha: ${pago.fecha}",
                    fontSize = 12.sp,
                    color = TextoSecundarioColor
                )
            }

            if (pago.periodoInicio.isNotBlank() || pago.periodoFin.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Periodo: ${pago.periodoInicio} al ${pago.periodoFin}",
                    fontSize = 12.sp,
                    color = TextoSecundarioColor
                )
            }

            if (pago.referencia.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ref: ${pago.referencia}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoPrincipalColor
                )
            }

            if (pago.nota.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "\"${pago.nota}\"",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Requerimiento 6: Muestra "Registrado por [nombre]" (14sp+)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!esAutor) {
                        Icon(Icons.Default.Lock, contentDescription = "Solo lectura", modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "Registrado por ${pago.registradoPorNombre.ifBlank { "Familiar" }}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                }

                if (esAutor) {
                    Row {
                        TextButton(onClick = onEditar) {
                            Text("Editar", fontWeight = FontWeight.Bold, color = LavandaPrimary)
                        }
                        TextButton(onClick = onEliminar) {
                            Text("Eliminar", fontWeight = FontWeight.Bold, color = RojoAlerta)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioPagoDialog(
    initialPago: Pago?,
    cuidadorPreseleccionado: ResumenPagosCuidador?,
    miembrosCuidadores: List<GroupMemberInfo>,
    cuidadoresResumen: List<ResumenPagosCuidador>,
    currentUserId: String,
    currentUserName: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onGuardar: (Pago) -> Unit
) {
    val hoyStr = remember { LocalDate.now().toString() }

    var selectedCuidadorId by remember {
        mutableStateOf(
            initialPago?.cuidadorUserId?.takeIf { it.isNotBlank() }
                ?: cuidadorPreseleccionado?.cuidadorUserId
                ?: miembrosCuidadores.firstOrNull()?.member?.userId
                ?: ""
        )
    }

    var selectedCuidadorNombre by remember {
        mutableStateOf(
            initialPago?.cuidadorNombre?.takeIf { it.isNotBlank() }
                ?: cuidadorPreseleccionado?.cuidadorNombre
                ?: miembrosCuidadores.firstOrNull()?.userName
                ?: ""
        )
    }

    val montoInicialDefecto = if (initialPago != null && initialPago.monto > 0.0) {
        DecimalFormat("0.00", DecimalFormatSymbols(Locale.US)).format(initialPago.monto)
    } else if (cuidadorPreseleccionado != null && cuidadorPreseleccionado.pendiente > 0.0) {
        DecimalFormat("0.00", DecimalFormatSymbols(Locale.US)).format(cuidadorPreseleccionado.pendiente)
    } else {
        ""
    }

    var montoText by remember { mutableStateOf(montoInicialDefecto) }
    var fechaIso by remember { mutableStateOf(initialPago?.fecha?.takeIf { it.isNotBlank() } ?: hoyStr) }
    var periodoInicioIso by remember { mutableStateOf(initialPago?.periodoInicio?.takeIf { it.isNotBlank() } ?: hoyStr) }
    var periodoFinIso by remember { mutableStateOf(initialPago?.periodoFin?.takeIf { it.isNotBlank() } ?: hoyStr) }
    var metodo by remember { mutableStateOf(initialPago?.metodo ?: MetodoPago.EFECTIVO) }
    var referencia by remember { mutableStateOf(initialPago?.referencia ?: "") }
    var nota by remember { mutableStateOf(initialPago?.nota ?: "") }

    var errorMonto by remember { mutableStateOf<String?>(null) }
    var errorCuidador by remember { mutableStateOf<String?>(null) }
    var errorPeriodo by remember { mutableStateOf<String?>(null) }

    var showFechaDialog by remember { mutableStateOf(false) }
    var showPeriodoInicioDialog by remember { mutableStateOf(false) }
    var showPeriodoFinDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initialPago == null) "Registrar Pago a Cuidador" else "Editar Pago",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Selector de Cuidador (Requerimiento 1: Solo Cuidadores Externos)
                Column {
                    Text("Cuidador:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextoPrincipalColor)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (miembrosCuidadores.isEmpty()) {
                        Text("No hay cuidadores externos en el grupo.", fontSize = 12.sp, color = Color.Red)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(miembrosCuidadores, key = { it.member.userId }) { m ->
                                val isSel = selectedCuidadorId == m.member.userId
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedCuidadorId = m.member.userId
                                        selectedCuidadorNombre = m.userName
                                        errorCuidador = null
                                    },
                                    label = {
                                        Text(
                                            text = "${m.userName} (${obtenerPerfilBadgeEtiqueta(m.member.subtipo)})",
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                    errorCuidador?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
                }

                // Monto (prefijo S/, teclado decimal, > 0)
                OutlinedTextField(
                    value = montoText,
                    onValueChange = { input ->
                        if (input.isBlank() || input.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                            montoText = input
                            val v = input.toDoubleOrNull() ?: 0.0
                            if (input.isNotBlank() && v > 0.0) errorMonto = null
                        }
                    },
                    label = { Text("Monto a pagar") },
                    prefix = { Text("S/ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMonto != null,
                    supportingText = { errorMonto?.let { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Fecha de Pago
                OutlinedButton(
                    onClick = { showFechaDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Fecha de Pago: $fechaIso", fontSize = 13.sp)
                }

                // Periodo desde y hasta (hasta >= desde)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showPeriodoInicioDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Desde: $periodoInicioIso", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showPeriodoFinDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hasta: $periodoFinIso", fontSize = 12.sp)
                    }
                }
                errorPeriodo?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

                // Método de Pago (chips)
                Column {
                    Text("Método de Pago:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextoPrincipalColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MetodoPago.values()) { m ->
                            val isSel = metodo == m
                            FilterChip(
                                selected = isSel,
                                onClick = { metodo = m },
                                label = { Text(m.etiqueta, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Referencia (nro de operación)
                OutlinedTextField(
                    value = referencia,
                    onValueChange = { referencia = it },
                    label = { Text("Nro. de Operación / Referencia (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Nota
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota / Observaciones (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val montoVal = montoText.toDoubleOrNull() ?: 0.0
                            var valid = true

                            if (selectedCuidadorId.isBlank()) {
                                errorCuidador = "Debes seleccionar un cuidador."
                                valid = false
                            }

                            if (montoText.isBlank() || montoVal <= 0.0) {
                                errorMonto = "El monto es obligatorio y debe ser mayor a cero."
                                valid = false
                            }

                            if (periodoFinIso < periodoInicioIso) {
                                errorPeriodo = "La fecha 'Hasta' debe ser mayor o igual a 'Desde'."
                                valid = false
                            }

                            if (valid) {
                                val finalPago = (initialPago ?: Pago()).copy(
                                    cuidadorUserId = selectedCuidadorId,
                                    cuidadorNombre = selectedCuidadorNombre,
                                    monto = montoVal,
                                    fecha = fechaIso,
                                    periodoInicio = periodoInicioIso,
                                    periodoFin = periodoFinIso,
                                    metodo = metodo,
                                    referencia = referencia.trim(),
                                    nota = nota.trim(),
                                    registradoPorUserId = initialPago?.registradoPorUserId?.takeIf { it.isNotBlank() } ?: currentUserId,
                                    registradoPorNombre = initialPago?.registradoPorNombre?.takeIf { it.isNotBlank() } ?: currentUserName,
                                    fechaRegistro = initialPago?.fechaRegistro?.takeIf { it > 0L } ?: System.currentTimeMillis()
                                )
                                onGuardar(finalPago)
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Guardar Pago", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showFechaDialog) {
        SelectorFechaDialog(
            fechaInicial = try { LocalDate.parse(fechaIso) } catch (e: Exception) { LocalDate.now() },
            titulo = "Fecha del Pago",
            onFechaSeleccionada = { fechaIso = it.toString(); showFechaDialog = false },
            onCancelar = { showFechaDialog = false }
        )
    }

    if (showPeriodoInicioDialog) {
        SelectorFechaDialog(
            fechaInicial = try { LocalDate.parse(periodoInicioIso) } catch (e: Exception) { LocalDate.now() },
            titulo = "Inicio del Periodo",
            onFechaSeleccionada = { periodoInicioIso = it.toString(); showPeriodoInicioDialog = false },
            onCancelar = { showPeriodoInicioDialog = false }
        )
    }

    if (showPeriodoFinDialog) {
        SelectorFechaDialog(
            fechaInicial = try { LocalDate.parse(periodoFinIso) } catch (e: Exception) { LocalDate.now() },
            titulo = "Fin del Periodo",
            fechaMinima = try { LocalDate.parse(periodoInicioIso) } catch (e: Exception) { null },
            onFechaSeleccionada = { periodoFinIso = it.toString(); showPeriodoFinDialog = false },
            onCancelar = { showPeriodoFinDialog = false }
        )
    }
}

@Composable
fun DetalleTurnosDialog(
    resumen: ResumenPagosCuidador,
    onDismiss: () -> Unit,
    onMarcarCumplida: (OcurrenciaDetalle) -> Unit,
    onMarcarNoCumplida: (OcurrenciaDetalle) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Detalle de Turnos", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Cuidador: ${resumen.cuidadorNombre}", fontSize = 13.sp, color = TextoSecundarioColor)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = LavandaPrimary)
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                if (resumen.ocurrencias.isEmpty()) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No hay ocurrencias registradas para este mes.", color = TextoSecundarioColor, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(resumen.ocurrencias) { ocurrencia ->
                            ItemOcurrenciaDetalle(
                                ocurrencia = ocurrencia,
                                onMarcarCumplida = { onMarcarCumplida(ocurrencia) },
                                onMarcarNoCumplida = { onMarcarNoCumplida(ocurrencia) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ItemOcurrenciaDetalle(
    ocurrencia: OcurrenciaDetalle,
    onMarcarCumplida: () -> Unit,
    onMarcarNoCumplida: () -> Unit
) {
    Surface(
        color = Color(0xFFF9F9F9),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fecha: ${ocurrencia.fechaIso}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipalColor
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (ocurrencia.esReemplazo) {
                        Surface(
                            color = Color(0xFFE1F5FE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🔄 Reemplazo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0288D1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val colorBadge = when (ocurrencia.estadoCodigo) {
                        "CUMPLIDA" -> Color(0xFFE8F5E9)
                        "SIN_CHECKIN" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFFFEBEE)
                    }
                    val colorTextoBadge = when (ocurrencia.estadoCodigo) {
                        "CUMPLIDA" -> Color(0xFF2E7D32)
                        "SIN_CHECKIN" -> Color(0xFFE65100)
                        else -> Color(0xFFC62828)
                    }

                    Surface(
                        color = colorBadge,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = ocurrencia.estadoEtiqueta,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorTextoBadge,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Horario: ${ocurrencia.horaInicio} - ${ocurrencia.horaFin} (${ocurrencia.horas.toInt()}h)",
                    fontSize = 12.sp,
                    color = TextoSecundarioColor
                )

                val textoMonto = if (ocurrencia.tarifaPorHora != null) {
                    formatearSoles(ocurrencia.montoDevengado)
                } else {
                    "Sin tarifa"
                }

                Text(
                    text = textoMonto,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ocurrencia.tarifaPorHora != null) MaterialTheme.colorScheme.primary else Color(0xFFE65100)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onMarcarNoCumplida,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Marcar no cumplida", fontSize = 11.sp, color = RojoAlerta, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onMarcarCumplida,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Marcar cumplida", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun exportarPagosCsv(context: Context, pagos: List<Pago>) {
    if (pagos.isEmpty()) {
        Toast.makeText(context, "No hay pagos registrados en este periodo.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val dfPen = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
        val sb = StringBuilder()
        sb.append("ID,Fecha,Cuidador,Monto (PEN),Moneda,PeriodoInicio,PeriodoFin,Metodo,Referencia,Nota,RegistradoPor\n")

        pagos.forEach { p ->
            val montoPen = dfPen.format(p.monto)
            sb.append("\"${p.id}\",\"${p.fecha}\",\"${p.cuidadorNombre}\",$montoPen,\"PEN\",\"${p.periodoInicio}\",\"${p.periodoFin}\",\"${p.metodo.etiqueta}\",\"${p.referencia}\",\"${p.nota}\",\"${p.registradoPorNombre}\"\n")
        }

        val timeStamp = LocalDate.now().toString()
        val fileName = "CuidaFamily_Pagos_$timeStamp.csv"
        val csvContent = sb.toString()

        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            insertUri?.also { u ->
                resolver.openOutputStream(u)?.use { os ->
                    os.write(csvContent.toByteArray(Charsets.UTF_8))
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { os ->
                os.write(csvContent.toByteArray(Charsets.UTF_8))
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        if (uri != null) {
            Toast.makeText(context, "CSV exportado a Descargas: $fileName", Toast.LENGTH_LONG).show()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Registro de Pagos (CSV - PEN)"))
        } else {
            Toast.makeText(context, "No se pudo generar el archivo CSV.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("PagosExport", "Error exportando CSV", e)
        Toast.makeText(context, "Error al exportar CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

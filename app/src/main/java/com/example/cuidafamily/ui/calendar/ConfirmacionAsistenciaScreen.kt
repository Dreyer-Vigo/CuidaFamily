package com.example.cuidafamily.ui.calendar

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cuidafamily.model.CalendarEvent
import com.example.cuidafamily.model.EstadoConfirmacion
import com.example.cuidafamily.model.Patient
import com.example.cuidafamily.model.calcularEdad
import com.example.cuidafamily.repository.CalendarRepository
import com.example.cuidafamily.repository.PatientRepository
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.ui.sos.SosFloatingButton
import com.example.cuidafamily.ui.theme.GradientEnd
import com.example.cuidafamily.ui.theme.GradientStart
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.theme.LavandaPrimaryLight
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.UserAvatar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

// TODO: migrar a Firebase Storage.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmacionAsistenciaScreen(
    familyGroupId: String,
    eventId: String,
    currentUserId: String,
    userName: String,
    onBackClick: () -> Unit,
    onSosClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var event by remember { mutableStateOf<CalendarEvent?>(null) }
    var patient by remember { mutableStateOf<Patient?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var showRechazoDialog by remember { mutableStateOf(false) }
    var showReemplazoDialog by remember { mutableStateOf(false) }

    val gradientStart = Color(0xFFFF6B9D)
    val gradientEnd = Color(0xFF7C4DFF)
    val actionBrush = Brush.horizontalGradient(listOf(gradientStart, gradientEnd))

    LaunchedEffect(familyGroupId, eventId) {
        isLoading = true
        event = CalendarRepository.obtenerEventoPorId(familyGroupId, eventId)
        val pResult = PatientRepository.obtenerPatientDelGrupo(familyGroupId)
        if (pResult is Result.Success) {
            patient = pResult.data
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Confirmar Asistencia", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        patient?.let { p ->
                            Text("Para: ${p.nombre} (${p.calcularEdad()} años)", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = LavandaPrimary
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        UserAvatar(nombre = userName, size = 36.dp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            SosFloatingButton(onClick = onSosClick)
        }
    ) { innerPadding ->
        AppBackgroundDecorated {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LavandaPrimary)
                    }
                } else if (event == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Evento no encontrado o eliminado.", color = Color.Gray)
                    }
                } else {
                    val ev = event!!
                    val isExpired = ev.fechaLimiteRespuesta != null && System.currentTimeMillis() > ev.fechaLimiteRespuesta

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val (badgeBg, badgeText, badgeLabel) = when (ev.confirmacionAsistencia) {
                                        EstadoConfirmacion.PENDIENTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "⏳ Respuesta Pendiente")
                                        EstadoConfirmacion.CONFIRMADO -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "✅ Asistencia Confirmada")
                                        EstadoConfirmacion.RECHAZADO -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "❌ No Podrás Asistir")
                                        EstadoConfirmacion.REEMPLAZO_SOLICITADO -> Triple(Color(0xFFEDE7F6), Color(0xFF673AB7), "🔄 Reemplazo Solicitado")
                                    }

                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            color = badgeText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    ev.fechaLimiteRespuesta?.let { limitMs ->
                                        val deadlineText = formatDeadlineText(limitMs)
                                        val deadlineBg = if (isExpired) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                                        val deadlineColor = if (isExpired) Color(0xFFC62828) else Color(0xFFF57F17)

                                        Surface(
                                            color = deadlineBg,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = deadlineText,
                                                color = deadlineColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = ev.titulo,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ev.fotoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ev.fotoUrl,
                                            contentDescription = "Foto del lugar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }

                                    if (ev.lugar.isNotBlank()) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(ev.lugar, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Surface(
                                    color = LavandaPrimaryLight,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = LavandaPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Fecha: ${ev.fecha} (${ev.horaInicio.format(timeFormat)} - ${ev.horaFin.format(timeFormat)})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = LavandaPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            val rolStr = if (ev.rolAsignado.isNotBlank()) " (${ev.rolAsignado})" else ""
                                            Text("Asignado a: ${ev.asignadoANombre ?: "Sin asignar"}$rolStr", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                if (!ev.notas.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        color = Color(0xFFFFF8E1),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFFE082)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Recordatorio e Indicaciones:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFF57F17))
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("\"${ev.notas}\"", fontSize = 13.sp, color = Color(0xFF5D4037))
                                            }
                                        }
                                    }
                                }

                                if (ev.motivoRechazo.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Motivo reportado: \"${ev.motivoRechazo}\"", fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                val canRespond = (!isExpired || ev.confirmacionAsistencia == EstadoConfirmacion.PENDIENTE) && ev.asignadoAUserId == currentUserId

                                if (canRespond) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                isSaving = true
                                                coroutineScope.launch {
                                                    CalendarRepository.actualizarConfirmacionAsistencia(familyGroupId, ev.id, EstadoConfirmacion.CONFIRMADO, "")
                                                    Toast.makeText(context, "¡Asistencia confirmada!", Toast.LENGTH_SHORT).show()
                                                    isSaving = false
                                                    onBackClick()
                                                }
                                            },
                                            enabled = !isSaving,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .background(actionBrush, RoundedCornerShape(14.dp))
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sí, confirmo asistencia", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                        }

                                        OutlinedButton(
                                            onClick = { showRechazoDialog = true },
                                            enabled = !isSaving,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                            border = BorderStroke(1.5.dp, Color(0xFFC62828)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFC62828))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("No puedo asistir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { showReemplazoDialog = true },
                                            enabled = !isSaving,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF673AB7)),
                                            border = BorderStroke(1.5.dp, Color(0xFF673AB7)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Text("Solicitar reemplazo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRechazoDialog) {
        DialogMotivoConfirmacion(
            titulo = "No puedo asistir",
            subtitulo = "Indica un motivo opcional para informar a la familia:",
            confirmColor = Color(0xFFC62828),
            onDismiss = { showRechazoDialog = false },
            onConfirm = { motivo ->
                event?.id?.let { id ->
                    coroutineScope.launch {
                        CalendarRepository.actualizarConfirmacionAsistencia(familyGroupId, id, EstadoConfirmacion.RECHAZADO, motivo)
                        Toast.makeText(context, "Respuesta enviada: No podrá asistir", Toast.LENGTH_SHORT).show()
                        onBackClick()
                    }
                }
                showRechazoDialog = false
            }
        )
    }

    if (showReemplazoDialog) {
        DialogMotivoConfirmacion(
            titulo = "Solicitar reemplazo",
            subtitulo = "Explica la razón por la que requieres un reemplazo:",
            confirmColor = Color(0xFF673AB7),
            onDismiss = { showReemplazoDialog = false },
            onConfirm = { motivo ->
                event?.id?.let { id ->
                    coroutineScope.launch {
                        CalendarRepository.actualizarConfirmacionAsistencia(familyGroupId, id, EstadoConfirmacion.REEMPLAZO_SOLICITADO, motivo)
                        Toast.makeText(context, "Solicitud de reemplazo enviada al Administrador", Toast.LENGTH_SHORT).show()
                        onBackClick()
                    }
                }
                showReemplazoDialog = false
            }
        )
    }
}

@Composable
fun DialogMotivoConfirmacion(
    titulo: String,
    subtitulo: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivoText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitulo, fontSize = 13.sp, color = Color.DarkGray)
                OutlinedTextField(
                    value = motivoText,
                    onValueChange = { motivoText = it },
                    label = { Text("Motivo (opcional)") },
                    placeholder = { Text("Ej: Conflicto de horario / Inconveniente personal") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(motivoText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enviar Respuesta", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

private fun formatDeadlineText(limitMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = limitMs - now
    if (diff <= 0) return "⚠️ Vencido"

    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val calNow = Calendar.getInstance()
    val calLimit = Calendar.getInstance().apply { timeInMillis = limitMs }

    return if (calNow.get(Calendar.YEAR) == calLimit.get(Calendar.YEAR) &&
        calNow.get(Calendar.DAY_OF_YEAR) == calLimit.get(Calendar.DAY_OF_YEAR)) {
        "⏰ Vence hoy ${sdfTime.format(Date(limitMs))}"
    } else {
        calNow.add(Calendar.DAY_OF_YEAR, 1)
        if (calNow.get(Calendar.YEAR) == calLimit.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calLimit.get(Calendar.DAY_OF_YEAR)) {
            "⏰ Vence mañana ${sdfTime.format(Date(limitMs))}"
        } else {
            val sdfDate = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            "⏰ Vence el ${sdfDate.format(Date(limitMs))}"
        }
    }
}

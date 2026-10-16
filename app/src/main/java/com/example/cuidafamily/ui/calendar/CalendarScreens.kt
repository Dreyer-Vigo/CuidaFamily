package com.example.cuidafamily.ui.calendar

import android.Manifest
import android.util.Log
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cuidafamily.model.CalendarEvent
import com.example.cuidafamily.model.EstadoConfirmacion
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.TipoEvento
import com.example.cuidafamily.repository.GroupMemberInfo
import com.example.cuidafamily.ui.sos.SosFloatingButton
import com.example.cuidafamily.ui.theme.AmbarAlerta
import com.example.cuidafamily.ui.theme.GradientEnd
import com.example.cuidafamily.ui.theme.GradientStart
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.theme.LavandaPrimaryLight
import com.example.cuidafamily.ui.theme.TextoSecundario
import com.example.cuidafamily.ui.theme.VerdeExito
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.GradientButton
import com.example.cuidafamily.ui.util.UserAvatar
import com.example.cuidafamily.ui.util.WheelTimePicker
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreenContainer(
    viewModel: CalendarViewModel,
    familyGroupId: String,
    userRole: Role,
    userName: String,
    onBackClick: () -> Unit,
    onNavigateToConfirmacion: (String) -> Unit = {},
    onSosClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventToReassign by remember { mutableStateOf<CalendarEvent?>(null) }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(familyGroupId, userRole) {
        viewModel.inicializarCalendario(familyGroupId, userRole)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var selectedViewIndex by remember { mutableStateOf(0) }
    val viewOptions = listOf("Este día", "Todos los eventos")

    val fabInteractionSource = remember { MutableInteractionSource() }
    val isPressed by fabInteractionSource.collectIsPressedAsState()
    val fabScale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "fabScale")
    val fabGradient = Brush.horizontalGradient(listOf(GradientStart, GradientEnd))

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = LavandaPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calendario", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.canWrite) {
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        interactionSource = fabInteractionSource,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .scale(fabScale)
                            .size(56.dp)
                            .background(fabGradient, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Agendar Actividad",
                            tint = Color.White
                        )
                    }
                }
                SosFloatingButton(onClick = onSosClick)
            }
        }
    ) { innerPadding ->
        AppBackgroundDecorated {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CalendarGridHeader(
                    anio = uiState.anioVisible,
                    mes = uiState.mesVisible,
                    diaSeleccionadoIso = uiState.diaSeleccionado,
                    eventosMesMap = uiState.eventosDelMesMap,
                    onPrevMonth = { viewModel.cambiarMes(false) },
                    onNextMonth = { viewModel.cambiarMes(true) },
                    onDiaSelected = { fechaIso -> viewModel.seleccionarDia(fechaIso) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    viewOptions.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = viewOptions.size),
                            onClick = { selectedViewIndex = index },
                            selected = selectedViewIndex == index,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = LavandaPrimary,
                                activeContentColor = Color.White,
                                inactiveContainerColor = Color.White.copy(alpha = 0.6f),
                                inactiveContentColor = LavandaPrimary
                            )
                        ) {
                            Text(label, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val tituloSeccion = if (selectedViewIndex == 0) "Eventos Asignados" else "Todos los eventos de ${obtenerNombreMes(uiState.mesVisible)}"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = tituloSeccion,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val eventosAMostrar = if (selectedViewIndex == 0) uiState.eventosDelDiaSeleccionado else uiState.eventosDelMesLista

                    Crossfade(targetState = eventosAMostrar, label = "eventsList") { events ->
                        if (events.isEmpty()) {
                            val emptyMsg = if (selectedViewIndex == 0) "No hay eventos este día" else "No hay eventos registrados este mes"
                            Surface(
                                color = Color.White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(emptyMsg, color = TextoSecundario, fontSize = 14.sp)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                events.forEach { event ->
                                    TarjetaEventoAnimada(
                                        event = event,
                                        mostrarFecha = selectedViewIndex == 1,
                                        canWrite = uiState.canWrite,
                                        currentUserId = currentUserId,
                                        onCompletarClick = { viewModel.completarEvento(event.id) },
                                        onResponderAsistencia = { estado, motivo ->
                                            viewModel.responderAsistencia(event.id, estado, motivo)
                                        },
                                        onNavigateToConfirmacion = { onNavigateToConfirmacion(event.id) },
                                        onReasignarClick = { eventToReassign = event },
                                        onEditClick = { eventToEdit = event },
                                        onDeleteClick = { eventToDelete = event },
                                        onClick = {
                                            if (selectedViewIndex == 1) {
                                                viewModel.seleccionarDia(event.fecha)
                                                selectedViewIndex = 0
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }

            if (showCreateDialog || eventToEdit != null) {
                FormularioCrearEventoDialog(
                    diaSeleccionado = uiState.diaSeleccionado,
                    initialEvent = eventToEdit,
                    onDismiss = { 
                        showCreateDialog = false
                        eventToEdit = null
                    },
                    onSave = { titulo, tipo, fecha, horaI, horaF, nota, lugar, rolAsignado, fotoUrl ->
                        if (eventToEdit != null) {
                            viewModel.modificarEvento(
                                eventToEdit!!.copy(
                                    titulo = titulo,
                                    tipo = tipo,
                                    fecha = fecha,
                                    horaInicio = horaI,
                                    horaFin = horaF,
                                    notas = nota.ifBlank { null },
                                    lugar = lugar,
                                    rolAsignado = rolAsignado,
                                    fotoUrl = fotoUrl
                                )
                            )
                        } else {
                            viewModel.agregarEvento(
                                titulo = titulo,
                                tipo = tipo,
                                fecha = fecha,
                                horaInicio = horaI,
                                horaFin = horaF,
                                notas = nota.ifBlank { null },
                                asignadoAUserId = null,
                                asignadoANombre = null,
                                asignadoARol = null,
                                creadoPorUserId = currentUserId,
                                lugar = lugar,
                                rolAsignado = rolAsignado,
                                fotoUrl = fotoUrl
                            )
                        }
                        showCreateDialog = false
                        eventToEdit = null
                    }
                )
            }

            if (eventToDelete != null) {
                EliminarEventoConfirmDialog(
                    onDismiss = { eventToDelete = null },
                    onConfirm = {
                        viewModel.borrarEvento(eventToDelete!!.id)
                        eventToDelete = null
                    }
                )
            }
        }
    }
}

@Composable
fun TarjetaEventoAnimada(
    event: CalendarEvent, 
    mostrarFecha: Boolean = false,
    canWrite: Boolean = false,
    currentUserId: String = "",
    onCompletarClick: () -> Unit,
    onResponderAsistencia: (EstadoConfirmacion, String) -> Unit = { _, _ -> },
    onNavigateToConfirmacion: (String) -> Unit = {},
    onReasignarClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    val accentColor = obtenerColorPorTipo(event.tipo)
    
    var showMenu by remember { mutableStateOf(false) }
    var showMotivoDialog by remember { mutableStateOf(false) }
    val esAdminOColab = canWrite

    val esAsignadoAUser = !event.asignadoAUserId.isNullOrBlank() && event.asignadoAUserId == currentUserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (esAdminOColab) {
                    Modifier.clickable {
                        Log.d("CalendarScreens", "Tarjeta de evento tocada: ${event.titulo}, rol: $canWrite")
                        showMenu = true
                    }
                } else if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(accentColor)
                )
                
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).background(accentColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (event.tipo == TipoEvento.CITA_MEDICA) Icons.Default.Schedule else Icons.Default.Groups,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (mostrarFecha) {
                            Text(
                                text = formatearFechaLegible(event.fecha),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        Text(
                            text = event.titulo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (event.completado) Color.Gray else MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = TextoSecundario, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                            val rangeText = "${event.horaInicio.format(timeFormatter)} - ${event.horaFin.format(timeFormatter)}"
                            Text(text = rangeText, fontSize = 12.sp, color = TextoSecundario)
                        }

                        if (!event.asignadoANombre.isNullOrBlank()) {
                            Text(
                                text = "Asignado a: ${event.asignadoANombre}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Badges de Confirmación de Asistencia
                        when (event.confirmacionAsistencia) {
                            EstadoConfirmacion.CONFIRMADO -> {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                                    Text("✅ Confirmado", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            EstadoConfirmacion.RECHAZADO -> {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column {
                                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                                        Text("❌ No podrá asistir", color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                    if (event.motivoRechazo.isNotBlank()) {
                                        Text("Motivo: \"${event.motivoRechazo}\"", color = Color(0xFFC62828).copy(alpha = 0.8f), fontSize = 10.sp)
                                    }
                                }
                            }
                            EstadoConfirmacion.REEMPLAZO_SOLICITADO -> {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column {
                                    Surface(color = Color(0xFFEDE7F6), shape = RoundedCornerShape(8.dp)) {
                                        Text("🔄 Solicita reemplazo", color = Color(0xFF673AB7), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                    if (event.motivoRechazo.isNotBlank()) {
                                        Text("Motivo: \"${event.motivoRechazo}\"", color = Color(0xFF673AB7).copy(alpha = 0.8f), fontSize = 10.sp)
                                    }
                                }
                            }
                            EstadoConfirmacion.PENDIENTE -> {
                                if (esAsignadoAUser) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { onNavigateToConfirmacion(event.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Responder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else if (!event.asignadoAUserId.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(8.dp)) {
                                        Text("⏳ Pendiente", color = Color(0xFFE65100), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!event.completado) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = { onCompletarClick() },
                            colors = CheckboxDefaults.colors(checkedColor = LavandaPrimary)
                        )
                    } else {
                        Text("✓", color = VerdeExito, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Menú de opciones para Admin/Colaborador
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text("Editar", fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = LavandaPrimary) },
                    onClick = {
                        showMenu = false
                        onEditClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    }
                )
            }
        }
    }

    if (showMotivoDialog) {
        MotivoRechazoDialog(
            onDismiss = { showMotivoDialog = false },
            onConfirm = { motivo ->
                onResponderAsistencia(EstadoConfirmacion.RECHAZADO, motivo)
                showMotivoDialog = false
            }
        )
    }
}

@Composable
fun EliminarEventoConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar evento", fontWeight = FontWeight.Bold) },
        text = { Text("Esta acción quitará el evento del calendario compartido de la familia. No se puede deshacer.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun MotivoRechazoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivoText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No puedo asistir", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Indica un motivo breve para que el grupo familiar lo tenga en cuenta (opcional):",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                OutlinedTextField(
                    value = motivoText,
                    onValueChange = { motivoText = it },
                    label = { Text("Motivo / Explicación") },
                    placeholder = { Text("Ej: Tengo cita médica / Inconveniente de horario") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(motivoText.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enviar Respuesta", fontWeight = FontWeight.Bold)
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

private fun formatearFechaLegible(fechaIso: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = input.parse(fechaIso) ?: return fechaIso
        val output = SimpleDateFormat("EEE d MMM", Locale.forLanguageTag("es-ES"))
        output.format(date).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        fechaIso
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioCrearEventoDialog(
    diaSeleccionado: String,
    initialEvent: CalendarEvent? = null,
    onDismiss: () -> Unit,
    onSave: (String, TipoEvento, String, LocalTime, LocalTime, String, String, String, String) -> Unit
) {
    val isEditing = initialEvent != null
    var titulo by remember { mutableStateOf(initialEvent?.titulo ?: "") }
    var tipo by remember { mutableStateOf(initialEvent?.tipo ?: TipoEvento.CITA_MEDICA) }
    var fechaSeleccionada by remember { mutableStateOf(initialEvent?.fecha ?: diaSeleccionado) }
    var horaInicio by remember { mutableStateOf(initialEvent?.horaInicio ?: LocalTime.of(9, 0)) }
    var horaFin by remember { mutableStateOf(initialEvent?.horaFin ?: LocalTime.of(10, 0)) }
    var notas by remember { mutableStateOf(initialEvent?.notas ?: "") }
    var lugar by remember { mutableStateOf(initialEvent?.lugar ?: "") }
    var rolAsignado by remember { mutableStateOf(initialEvent?.rolAsignado ?: "") }
    var fotoUrl by remember { mutableStateOf(initialEvent?.fotoUrl ?: "") }
    
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePickerInicio by remember { mutableStateOf(false) }
    var showTimePickerFin by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { fotoUrl = it.toString() }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fechaSeleccionada)?.time
        } catch(e: Exception) { System.currentTimeMillis() }
    )

    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isEditing) "Editar Actividad" else "Agendar Nueva Actividad", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 20.sp, 
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título de la actividad") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = tipo.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría de Evento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                        TipoEvento.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) }, onClick = { tipo = t; dropdownExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = lugar,
                    onValueChange = { lugar = it },
                    label = { Text("Lugar (opcional)") },
                    placeholder = { Text("Ej: Clínica San Pablo / Parque") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = rolAsignado,
                    onValueChange = { rolAsignado = it },
                    label = { Text("Rol designado (opcional)") },
                    placeholder = { Text("Ej: Conductor y Acompañante") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = fechaSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha") },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = Color.Gray, disabledTextColor = Color.DarkGray),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = horaInicio.format(timeFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Inicio") },
                        modifier = Modifier.weight(1f).clickable { showTimePickerInicio = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = Color.Gray, disabledTextColor = Color.DarkGray),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = horaFin.format(timeFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fin") },
                        modifier = Modifier.weight(1f).clickable { showTimePickerFin = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = Color.Gray, disabledTextColor = Color.DarkGray),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas / Indicaciones (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = LavandaPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fotoUrl.isNotBlank()) "Cambiar Foto del Lugar" else "Agregar Foto del Lugar (opcional)", color = LavandaPrimary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = TextoSecundario) }
                    Spacer(modifier = Modifier.width(8.dp))
                    GradientButton(
                        text = "Guardar",
                        onClick = { onSave(titulo, tipo, fechaSeleccionada, horaInicio, horaFin, notas, lugar, rolAsignado, fotoUrl) },
                        enabled = titulo.isNotBlank()
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = ms }
                        fechaSeleccionada = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(utcCalendar.time)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePickerInicio) {
        var tempTime by remember { mutableStateOf(horaInicio) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePickerInicio = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Hora de Inicio", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LavandaPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    WheelTimePicker(horaInicial = tempTime, onTimeChanged = { tempTime = it })
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { horaInicio = tempTime; showTimePickerInicio = false },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Aceptar") }
                }
            }
        }
    }

    if (showTimePickerFin) {
        var tempTime by remember { mutableStateOf(horaFin) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePickerFin = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Hora de Término", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LavandaPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    WheelTimePicker(horaInicial = tempTime, onTimeChanged = { tempTime = it })
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { horaFin = tempTime; showTimePickerFin = false },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Aceptar") }
                }
            }
        }
    }
}

@Composable
fun CalendarGridHeader(
    anio: Int,
    mes: Int,
    diaSeleccionadoIso: String,
    eventosMesMap: Map<String, List<CalendarEvent>>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDiaSelected: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes Anterior", tint = LavandaPrimary)
                }
                Text(
                    text = "${obtenerNombreMes(mes)} $anio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes Siguiente", tint = LavandaPrimary, modifier = Modifier.scale(scaleX = -1f, scaleY = 1f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                val diasSemanaHeader = listOf("D", "L", "M", "M", "J", "V", "S")
                diasSemanaHeader.forEach { dia ->
                    Text(
                        text = dia,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoSecundario,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val matrizDias = calcularMatrizDiasMes(mes, anio)
            matrizDias.forEach { semana ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    semana.forEach { diaNum ->
                        if (diaNum == null) {
                            Spacer(modifier = Modifier.width(36.dp))
                        } else {
                            val fechaIso = String.format(Locale.US, "%04d-%02d-%02d", anio, mes, diaNum)
                            val isSelected = fechaIso == diaSeleccionadoIso
                            val tieneEventos = eventosMesMap.containsKey(fechaIso)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) LavandaPrimary else Color.Transparent
                                    )
                                    .clickable { onDiaSelected(fechaIso) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = diaNum.toString(),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (tieneEventos && !isSelected) {
                                        val primerEvento = eventosMesMap[fechaIso]?.firstOrNull()
                                        val colorPunto = obtenerColorPorTipo(primerEvento?.tipo ?: TipoEvento.OTRO)
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(colorPunto, CircleShape)
                                        )
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

private fun obtenerNombreMes(mes: Int): String {
    val nombres = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    return nombres.getOrElse(mes - 1) { "" }
}

private fun calcularMatrizDiasMes(mes: Int, anio: Int): List<List<Int?>> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, anio)
        set(Calendar.MONTH, mes - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val primerDiaSemana = cal.get(Calendar.DAY_OF_WEEK) - 1
    val maxDias = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val resultado = mutableListOf<List<Int?>>()
    var filaActual = mutableListOf<Int?>()

    for (i in 0 until primerDiaSemana) {
        filaActual.add(null)
    }

    for (dia in 1..maxDias) {
        filaActual.add(dia)
        if (filaActual.size == 7) {
            resultado.add(filaActual)
            filaActual = mutableListOf()
        }
    }

    if (filaActual.isNotEmpty()) {
        while (filaActual.size < 7) {
            filaActual.add(null)
        }
        resultado.add(filaActual)
    }

    return resultado
}

private fun obtenerColorPorTipo(tipo: TipoEvento): Color {
    return when (tipo) {
        TipoEvento.CITA_MEDICA -> LavandaPrimary
        TipoEvento.TERAPIA -> VerdeExito
        TipoEvento.VISITA -> VerdeExito
        TipoEvento.OTRO -> AmbarAlerta
    }
}

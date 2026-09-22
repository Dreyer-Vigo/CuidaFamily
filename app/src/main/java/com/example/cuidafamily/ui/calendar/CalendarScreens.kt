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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.TipoEvento
import com.example.cuidafamily.ui.theme.AmbarAlerta
import com.example.cuidafamily.ui.theme.GradientEnd
import com.example.cuidafamily.ui.theme.GradientStart
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.theme.LavandaPrimaryLight
import com.example.cuidafamily.ui.theme.TextoSecundario
import com.example.cuidafamily.ui.theme.VerdeExito
import com.example.cuidafamily.ui.util.GradientButton
import com.example.cuidafamily.ui.util.UserAvatar
import com.example.cuidafamily.ui.util.WheelTimePicker
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
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<CalendarEvent?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(familyGroupId, userRole) {
        viewModel.inicializarCalendario(familyGroupId, userRole)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navBgBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFFF3E8FF), Color(0xFFFFFFFF))
    )

    var selectedViewIndex by remember { mutableStateOf(0) } // 0: Este día, 1: Todos los eventos
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
            if (uiState.canWrite) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    interactionSource = fabInteractionSource,
                    modifier = Modifier
                        .scale(fabScale)
                        .size(56.dp)
                        .background(fabGradient, CircleShape)
                ) {
                    Text("+", fontSize = 28.sp, fontWeight = FontWeight.Normal)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(navBgBrush)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Selector de Mes Animado
            var slideDirection by remember { mutableStateOf(1) }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        slideDirection = -1
                        viewModel.cambiarMes(avanzar = false) 
                    }) {
                        Text("◀", color = LavandaPrimary, fontSize = 16.sp)
                    }
                    
                    AnimatedContent(
                        targetState = uiState.mesVisible to uiState.anioVisible,
                        transitionSpec = {
                            if (slideDirection > 0) {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }.using(SizeTransform(clip = false))
                        }, label = "monthTransition"
                    ) { (mes, anio) ->
                        Text(
                            text = obtenerNombreMes(mes) + " " + anio,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { 
                        slideDirection = 1
                        viewModel.cambiarMes(avanzar = true) 
                    }) {
                        Text("▶", color = LavandaPrimary, fontSize = 16.sp)
                    }
                }
                Text("Vista compartida familiar", fontSize = 13.sp, color = TextoSecundario)
            }

            // 2. Grilla
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    GrillaCalendario(
                        anio = uiState.anioVisible,
                        mes = uiState.mesVisible,
                        diaSeleccionado = uiState.diaSeleccionado,
                        eventosMesMap = uiState.eventosDelMesMap,
                        onDiaClick = { viewModel.seleccionarDia(it) }
                    )
                }
            }

            // 3. Selector de Vista (Este día / Todos los eventos)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                viewOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = viewOptions.size),
                        onClick = { selectedViewIndex = index },
                        selected = selectedViewIndex == index,
                        label = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = LavandaPrimary,
                            activeContentColor = Color.White,
                            inactiveContentColor = LavandaPrimary
                        )
                    )
                }
            }

            // 4. Lista de Eventos (Contextual)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val tituloSeccion = if (selectedViewIndex == 0) "Eventos Asignados" else "Todos los eventos de ${obtenerNombreMes(uiState.mesVisible)}"
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tituloSeccion, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (selectedViewIndex == 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(LavandaPrimaryLight, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Hoy", color = LavandaPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                val eventosAMostrar = if (selectedViewIndex == 0) uiState.eventosDelDiaSeleccionado else uiState.eventosDelMesLista

                Crossfade(targetState = eventosAMostrar, label = "eventsList") { events ->
                    if (events.isEmpty()) {
                        val emptyMsg = if (selectedViewIndex == 0) "No hay eventos este día" else "No hay eventos registrados este mes"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
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
                                    onCompletarClick = { viewModel.completarEvento(event.id) },
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
                onSave = { titulo, tipo, fecha, horaI, horaF, nota ->
                    if (eventToEdit != null) {
                        viewModel.modificarEvento(
                            eventToEdit!!.copy(
                                titulo = titulo,
                                tipo = tipo,
                                fecha = fecha,
                                horaInicio = horaI,
                                horaFin = horaF,
                                notas = nota.ifBlank { null }
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
                            creadoPorUserId = "CREATOR_ID" // Debería venir del AuthState real
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

@Composable
fun EliminarEventoConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Eliminar actividad?", fontWeight = FontWeight.Bold) },
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
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextoSecundario)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun GrillaCalendario(
    anio: Int,
    mes: Int,
    diaSeleccionado: String,
    eventosMesMap: Map<String, List<CalendarEvent>>,
    onDiaClick: (String) -> Unit
) {
    val diasSemana = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, anio)
        set(Calendar.MONTH, mes - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val diaInicioSemana = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val maxDias = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            diasSemana.forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = TextoSecundario
                )
            }
        }

        var diaContador = 1
        for (fila in 0..5) {
            if (diaContador > maxDias) break
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val indiceCelda = fila * 7 + col
                    if (indiceCelda < diaInicioSemana || diaContador > maxDias) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val diaActual = diaContador
                        val fechaIso = String.format(Locale.US, "%04d-%02d-%02d", anio, mes, diaActual)
                        val isSelected = fechaIso == diaSeleccionado
                        val tieneEventos = eventosMesMap.containsKey(fechaIso)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) LavandaPrimary else Color.Transparent)
                                    .clickable { onDiaClick(fechaIso) }
                                    .animateContentSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = diaActual.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.DarkGray
                                )
                                if (tieneEventos && !isSelected) {
                                    val primerEvento = eventosMesMap[fechaIso]?.firstOrNull()
                                    val colorPunto = obtenerColorPorTipo(primerEvento?.tipo ?: TipoEvento.OTRO)
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(8.dp) // Puntos más grandes
                                            .shadow(elevation = 4.dp, shape = CircleShape, spotColor = colorPunto) // Glow/Sombra
                                            .background(colorPunto, CircleShape)
                                    )
                                }
                            }
                        }
                        diaContador++
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaEventoAnimada(
    event: CalendarEvent, 
    mostrarFecha: Boolean = false,
    canWrite: Boolean = false,
    onCompletarClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    val accentColor = obtenerColorPorTipo(event.tipo)
    
    var showMenu by remember { mutableStateOf(false) }
    val esAdminOColab = canWrite

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
    onSave: (String, TipoEvento, String, LocalTime, LocalTime, String) -> Unit
) {
    val isEditing = initialEvent != null
    var titulo by remember { mutableStateOf(initialEvent?.titulo ?: "") }
    var tipo by remember { mutableStateOf(initialEvent?.tipo ?: TipoEvento.CITA_MEDICA) }
    var fechaSeleccionada by remember { mutableStateOf(initialEvent?.fecha ?: diaSeleccionado) }
    var horaInicio by remember { mutableStateOf(initialEvent?.horaInicio ?: LocalTime.of(9, 0)) }
    var horaFin by remember { mutableStateOf(initialEvent?.horaFin ?: LocalTime.of(10, 0)) }
    var notas by remember { mutableStateOf(initialEvent?.notas ?: "") }
    
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePickerInicio by remember { mutableStateOf(false) }
    var showTimePickerFin by remember { mutableStateOf(false) }

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
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
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
                    label = { Text("Notas (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = TextoSecundario) }
                    Spacer(modifier = Modifier.width(8.dp))
                    GradientButton(
                        text = "Guardar",
                        onClick = { onSave(titulo, tipo, fechaSeleccionada, horaInicio, horaFin, notas) },
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
                    Text("Seleccionar Hora Inicio", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    WheelTimePicker(
                        horaInicial = horaInicio,
                        onTimeChanged = { tempTime = it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePickerInicio = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        GradientButton(
                            text = "Confirmar",
                            onClick = {
                                horaInicio = tempTime
                                showTimePickerInicio = false
                            },
                            modifier = Modifier.height(44.dp)
                        )
                    }
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
                    Text("Seleccionar Hora Fin", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    WheelTimePicker(
                        horaInicial = horaFin,
                        onTimeChanged = { tempTime = it }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePickerFin = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        GradientButton(
                            text = "Confirmar",
                            onClick = {
                                horaFin = tempTime
                                showTimePickerFin = false
                            },
                            modifier = Modifier.height(44.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun obtenerColorPorTipo(tipo: TipoEvento): Color {
    return when (tipo) {
        TipoEvento.CITA_MEDICA -> LavandaPrimary
        TipoEvento.TERAPIA -> VerdeExito
        TipoEvento.VISITA -> VerdeExito
        TipoEvento.OTRO -> AmbarAlerta
    }
}

private fun obtenerNombreMes(mes: Int): String {
    return when (mes) {
        1 -> "Enero" 2 -> "Febrero" 3 -> "Marzo" 4 -> "Abril" 5 -> "Mayo" 6 -> "Junio"
        7 -> "Julio" 8 -> "Agosto" 9 -> "Septiembre" 10 -> "Octubre" 11 -> "Noviembre" 12 -> "Diciembre"
        else -> "Mes"
    }
}

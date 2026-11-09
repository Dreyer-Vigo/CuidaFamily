package com.example.cuidafamily.ui.medicacion

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cuidafamily.BuildConfig
import com.example.cuidafamily.model.*
import com.example.cuidafamily.ui.theme.*
import com.example.cuidafamily.ui.util.*
import com.example.cuidafamily.util.RecordatorioScheduler
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicacionScreenContainer(
    viewModel: MedicacionViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Hoy, 1: Medicamentos, 2: Historial

    var showMedDialog by remember { mutableStateOf(false) }
    var medToEdit by remember { mutableStateOf<Medicamento?>(null) }
    var dosisParaOmitir by remember { mutableStateOf<DosisItem?>(null) }
    var notaOmitida by remember { mutableStateOf("") }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refrescarDiagnostico()
    }

    LaunchedEffect(Unit) {
        viewModel.refrescarDiagnostico()
        if (state.familyGroupId.isNotBlank()) {
            RecordatorioScheduler.programarAlarmasProximas48H(
                context,
                state.familyGroupId,
                state.currentUserId,
                state.currentUserRole
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refrescarDiagnostico()
                val st = viewModel.uiState.value
                if (st.familyGroupId.isNotBlank()) {
                    RecordatorioScheduler.programarAlarmasProximas48H(
                        context,
                        st.familyGroupId,
                        st.currentUserId,
                        st.currentUserRole
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val gradientBrush = Brush.linearGradient(listOf(GradientStart, GradientEnd))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control de Medicación", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (selectedTab == 1 && (state.currentUserRole == Role.ADMINISTRADOR_FAMILIAR || state.currentUserRole == Role.COLABORADOR)) {
                FloatingActionButton(
                    onClick = {
                        medToEdit = null
                        showMedDialog = true
                    },
                    containerColor = LavandaPrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Medicamento")
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        AppBackgroundDecorated {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // PANEL "Estado de recordatorios" (plegable, visible para todos los roles)
                EstadoRecordatoriosPanel(
                    state = state,
                    viewModel = viewModel,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            abrirAjustesAppNotificaciones(context)
                        }
                    }
                )

                // Pestañas superiores personalizadas
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val tabs = listOf("Hoy", "Medicamentos", "Historial")
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(if (isSelected) Modifier.background(gradientBrush) else Modifier)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color.White else TextoPrincipalColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> TabHoyContent(
                            state = state,
                            onCambiarFecha = { viewModel.cambiarFecha(it) },
                            onConfirmarToma = { dosis ->
                                viewModel.confirmarToma(dosis) {}
                            },
                            onMarcarOmitida = { dosis ->
                                dosisParaOmitir = dosis
                                notaOmitida = ""
                            }
                        )
                        1 -> TabMedicamentosContent(
                            state = state,
                            onEditar = { med ->
                                medToEdit = med
                                showMedDialog = true
                            },
                            onDesactivar = { medId ->
                                viewModel.desactivarMedicamento(medId) {}
                            }
                        )
                        2 -> TabHistorialContent(state = state)
                    }
                }

                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grupo: ${state.familyGroupId}, medicamentos: ${state.medicamentos.size}, dosis hoy: ${state.dosisDelDia.size}, alarmas: ${state.alarmasProgramadasCount}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
            }
        }
    }

    if (showMedDialog) {
        MedicamentoFormDialog(
            medicamentoInicial = medToEdit,
            onDismiss = { showMedDialog = false },
            onGuardar = { med ->
                viewModel.crearOEditarMedicamento(med) {
                    showMedDialog = false
                }
            }
        )
    }

    if (dosisParaOmitir != null) {
        AlertDialog(
            onDismissRequest = { dosisParaOmitir = null },
            title = { Text("Marcar toma como omitida", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text("¿Deseas marcar la toma de ${dosisParaOmitir?.medicamentoNombre} a las ${dosisParaOmitir?.hora} como omitida?", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notaOmitida,
                        onValueChange = { notaOmitida = it },
                        label = { Text("Motivo / Nota (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dosis = dosisParaOmitir
                        if (dosis != null) {
                            viewModel.marcarOmitida(dosis, notaOmitida) {
                                dosisParaOmitir = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RojoAlerta),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Omitir toma", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { dosisParaOmitir = null }, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun EstadoRecordatoriosPanel(
    state: MedicacionUiState,
    viewModel: MedicacionViewModel,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val diag = state.diagnostico
    val todoOk = diag.notifPermissionGranted &&
            diag.appNotificationsEnabled &&
            diag.channelCreated &&
            !diag.channelSilenced &&
            diag.channelPreviaCreated &&
            !diag.channelPreviaSilenced &&
            diag.exactAlarmsGranted &&
            diag.receiverRegistered &&
            diag.personalRemindersEnabled

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (todoOk) Color(0xFF81C784) else LavandaPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (todoOk) Icons.Default.CheckCircle else Icons.Default.Tune,
                        contentDescription = null,
                        tint = if (todoOk) Color(0xFF2E7D32) else LavandaPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Estado de recordatorios", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextoPrincipalColor)
                        Text(
                            text = if (todoOk) "✅ Todos los permisos y canales listos" else "⚠️ Toca para revisar diagnóstico y permisos",
                            fontSize = 11.sp,
                            color = if (todoOk) Color(0xFF2E7D32) else RojoAlerta
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Plegar" else "Desplegar",
                        tint = LavandaPrimary
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                // 1. Permiso POST_NOTIFICATIONS
                DiagnosticoFila(
                    correcto = diag.notifPermissionGranted,
                    titulo = "Permiso de notificaciones (POST_NOTIFICATIONS)",
                    descripcion = if (diag.notifPermissionGranted) "Permiso concedido para notificaciones en Android 13+" else "Sin permiso para mostrar avisos de notificación",
                    actionButtonText = if (!diag.notifPermissionGranted) "Permitir" else null,
                    onActionClick = onRequestPermission
                )

                // 2. Notificaciones de la app activadas
                DiagnosticoFila(
                    correcto = diag.appNotificationsEnabled,
                    titulo = "Notificaciones de la app activadas",
                    descripcion = if (diag.appNotificationsEnabled) "Notificaciones habilitadas en ajustes del sistema" else "Las notificaciones están bloqueadas en los ajustes del sistema",
                    actionButtonText = if (!diag.appNotificationsEnabled) "Abrir ajustes" else null,
                    onActionClick = { abrirAjustesAppNotificaciones(context) }
                )

                // 3. Canal medicacion_recordatorios
                DiagnosticoFila(
                    correcto = diag.channelCreated && !diag.channelSilenced,
                    alerta = diag.channelSilenced,
                    titulo = "Canal 'medicacion_recordatorios'",
                    descripcion = when {
                        diag.channelSilenced -> "El usuario silenció o desactivó este canal en ajustes"
                        diag.channelCreated -> "Canal creado correctamente con importancia alta"
                        else -> "Canal no creado en el sistema"
                    },
                    actionButtonText = "Ajustes canal",
                    onActionClick = { abrirAjustesCanalNotificaciones(context) }
                )

                // 4. Canal medicacion_alarma_previa
                DiagnosticoFila(
                    correcto = diag.channelPreviaCreated && !diag.channelPreviaSilenced,
                    alerta = diag.channelPreviaSilenced,
                    titulo = "Canal de alarma previa",
                    descripcion = when {
                        diag.channelPreviaSilenced -> "Silenciado o desactivado por el usuario en ajustes"
                        diag.channelPreviaCreated -> "Canal activo con sonido de alarma persistente"
                        else -> "Canal no creado en el sistema"
                    },
                    actionButtonText = "Ajustes canal previo",
                    onActionClick = { abrirAjustesCanalPreviaNotificaciones(context) }
                )

                // 5. Permiso de alarmas exactas
                DiagnosticoFila(
                    correcto = diag.exactAlarmsGranted,
                    alerta = !diag.exactAlarmsGranted,
                    titulo = "Permiso de alarmas exactas",
                    descripcion = if (diag.exactAlarmsGranted) "Permiso concedido (SCHEDULE_EXACT_ALARM)" else "Sin permiso para alarmas exactas (se usarán inexactas)",
                    actionButtonText = if (!diag.exactAlarmsGranted) "Permitir alarmas" else null,
                    onActionClick = { abrirAjustesAlarmasExactas(context) }
                )

                // 6. Modo demostración
                DiagnosticoFila(
                    correcto = diag.demoModeActive,
                    titulo = "Modo demostración",
                    descripcion = if (diag.demoModeActive) "Activado (suena en este celular sin depender de servidor)" else "Desactivado",
                    trailingContent = {
                        Switch(
                            checked = diag.demoModeActive,
                            onCheckedChange = { active -> viewModel.setDemoModeActive(active) },
                            colors = SwitchDefaults.colors(checkedThumbColor = LavandaPrimary)
                        )
                    }
                )

                // 7. Interruptor personal "Recibir recordatorios"
                DiagnosticoFila(
                    correcto = diag.personalRemindersEnabled,
                    titulo = "Interruptor personal 'Recibir recordatorios'",
                    descripcion = if (diag.personalRemindersEnabled) "Activado para este celular" else "Desactivado (no sonarán alarmas locales)",
                    trailingContent = {
                        Switch(
                            checked = diag.personalRemindersEnabled,
                            onCheckedChange = { enabled -> viewModel.setPersonalRemindersEnabled(enabled) },
                            colors = SwitchDefaults.colors(checkedThumbColor = LavandaPrimary)
                        )
                    }
                )

                // 8. CONFIGURACIÓN DE AVISO PREVIO
                DiagnosticoFila(
                    correcto = diag.avisoPrevioActive,
                    titulo = "Aviso previo con alarma",
                    descripcion = if (diag.avisoPrevioActive) "Suena ${diag.anticipacionMinutos} min antes de cada dosis con sonido de alarma" else "Desactivado (solo suena a la hora exacta)",
                    trailingContent = {
                        Switch(
                            checked = diag.avisoPrevioActive,
                            onCheckedChange = { active -> viewModel.setAvisoPrevioActive(active) },
                            colors = SwitchDefaults.colors(checkedThumbColor = LavandaPrimary)
                        )
                    }
                )

                if (diag.avisoPrevioActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Anticipación del aviso:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10, 15).forEach { min ->
                                val isSel = diag.anticipacionMinutos == min
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.setAnticipacionMinutos(min) },
                                    label = { Text("$min min", fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // 9. Alarmas programadas
                DiagnosticoFila(
                    correcto = diag.scheduledAlarmsCount > 0,
                    alerta = diag.scheduledAlarmsCount == 0,
                    titulo = "Alarmas programadas: ${diag.scheduledAlarmsCount}",
                    descripcion = buildString {
                        append("Próxima: ")
                        append(diag.nextAlarmTime ?: "Ninguna")
                        if (diag.inexactAlarmsCount > 0) {
                            append("\n⚠️ Alarmas inexactas: ${diag.inexactAlarmsCount}")
                        }
                        append("\nDescartadas: ${diag.discardedPastCount} en el pasado, ${diag.discardedRecipientCount} para otro usuario")
                    }
                )

                // 10. Receiver registrado en AndroidManifest
                DiagnosticoFila(
                    correcto = diag.receiverRegistered,
                    titulo = "Receiver registrado en AndroidManifest",
                    descripcion = if (diag.receiverRegistered) "MedicacionAlarmReceiver declarado en AndroidManifest.xml" else "❌ Receiver no encontrado en AndroidManifest.xml"
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                Text("BOTONES DE PRUEBA", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LavandaPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                RecordatorioScheduler.probarNotificacionAhora(context)
                                snackbarMessage = "Notificación de prueba emitida inmediatamente"
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(15.dp), tint = LavandaPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Probar notificación", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                RecordatorioScheduler.programarPrueba10Segundos(context)
                                snackbarMessage = "Alarma en hora programada para en 10 segundos"
                                viewModel.refrescarDiagnostico()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(15.dp), tint = LavandaPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Probar en hora (10 s)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary, maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            RecordatorioScheduler.programarPruebaAvisoPrevio10Segundos(context)
                            snackbarMessage = "Aviso previo de prueba (alarma) programado para en 10 segundos"
                            viewModel.refrescarDiagnostico()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AlarmOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = LavandaPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probar aviso previo con alarma (10 s)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary)
                    }
                }

                if (snackbarMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = snackbarMessage!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticoFila(
    correcto: Boolean,
    alerta: Boolean = false,
    titulo: String,
    descripcion: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            val icon = when {
                correcto -> "✅"
                alerta -> "⚠️"
                else -> "❌"
            }
            Text(icon, fontSize = 15.sp, modifier = Modifier.padding(top = 1.dp, end = 8.dp))
            Column {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextoPrincipalColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(descripcion, fontSize = 11.sp, color = TextoSecundarioColor)
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        } else if (!actionButtonText.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(actionButtonText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

fun abrirAjustesAppNotificaciones(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}

fun abrirAjustesCanalNotificaciones(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, RecordatorioScheduler.CHANNEL_ID)
            }
            context.startActivity(intent)
        } else {
            abrirAjustesAppNotificaciones(context)
        }
    } catch (e: Exception) {
        abrirAjustesAppNotificaciones(context)
    }
}

fun abrirAjustesCanalPreviaNotificaciones(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, RecordatorioScheduler.CHANNEL_ALARMA_PREVIA_ID)
            }
            context.startActivity(intent)
        } else {
            abrirAjustesAppNotificaciones(context)
        }
    } catch (e: Exception) {
        abrirAjustesAppNotificaciones(context)
    }
}

fun abrirAjustesAlarmasExactas(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
}

@Composable
fun TabHoyContent(
    state: MedicacionUiState,
    onCambiarFecha: (LocalDate) -> Unit,
    onConfirmarToma: (DosisItem) -> Unit,
    onMarcarOmitida: (DosisItem) -> Unit
) {
    var showFechaDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onCambiarFecha(state.fechaActual.minusDays(1)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Día anterior", tint = LavandaPrimary)
                }

                TextButton(onClick = { showFechaDialog = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = LavandaPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.fechaActual == LocalDate.now()) "Hoy (${formatFechaEspanolCorta(state.fechaActual)})" else formatFechaEspanolCorta(state.fechaActual),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = { onCambiarFecha(state.fechaActual.plusDays(1)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Día siguiente", tint = LavandaPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.dosisDelDia.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No hay dosis programadas para hoy", color = TextoPrincipalColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    val activos = state.medicamentos.filter { it.activo }
                    if (activos.isNotEmpty()) {
                        Text("Medicamentos registrados:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        activos.forEach { med ->
                            val motivo = when {
                                med.fechaInicio.isNotBlank() && state.fechaActual.toString() < med.fechaInicio -> " (Empieza el ${med.fechaInicio})"
                                med.fechaFin != null && state.fechaActual.toString() > med.fechaFin -> " (Finalizó el ${med.fechaFin})"
                                else -> " (Sin horario o día no programado)"
                            }
                            Text("• ${med.nombre}$motivo", fontSize = 13.sp, color = TextoSecundarioColor)
                        }
                    } else {
                        Text("No hay medicamentos activos en el grupo.", fontSize = 14.sp, color = TextoSecundarioColor)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.dosisDelDia) { dosis ->
                    DosisCard(
                        dosis = dosis,
                        onConfirmar = { onConfirmarToma(dosis) },
                        onOmitir = { onMarcarOmitida(dosis) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showFechaDialog) {
        SelectorFechaDialog(
            fechaInicial = state.fechaActual,
            titulo = "Seleccionar día de medicación",
            onFechaSeleccionada = { nueva ->
                onCambiarFecha(nueva)
                showFechaDialog = false
            },
            onCancelar = { showFechaDialog = false }
        )
    }
}

@Composable
fun DosisCard(
    dosis: DosisItem,
    onConfirmar: () -> Unit,
    onOmitir: () -> Unit
) {
    val badgeColor = when (dosis.estadoCalculado) {
        EstadoDosis.PENDIENTE -> Color(0xFFE65100) // Ámbar
        EstadoDosis.CONFIRMADA -> Color(0xFF2E7D32) // Verde
        EstadoDosis.ATRASADA -> RojoAlerta // Rojo
        EstadoDosis.OMITIDA -> Color.Gray
    }

    val badgeText = when (dosis.estadoCalculado) {
        EstadoDosis.PENDIENTE -> "⏳ Pendiente"
        EstadoDosis.CONFIRMADA -> {
            val nombre = dosis.tomaGuardada?.confirmadoPorNombre ?: "Alguien"
            val horaC = if (dosis.tomaGuardada?.horaConfirmacion != null && dosis.tomaGuardada.horaConfirmacion > 0L) {
                SimpleDateFormat("HH:mm", Locale.US).format(Date(dosis.tomaGuardada.horaConfirmacion))
            } else ""
            "✅ Confirmada por $nombre${if (horaC.isNotBlank()) " a las $horaC" else ""}"
        }
        EstadoDosis.ATRASADA -> "⚠️ Atrasada"
        EstadoDosis.OMITIDA -> "⛔ Omitida"
    }

    val gradientBrush = Brush.linearGradient(listOf(GradientStart, GradientEnd))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dosis.hora,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dosis.medicamentoNombre,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipalColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Dosis: ${dosis.dosis}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextoSecundarioColor
            )

            if (dosis.indicaciones.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Indicaciones: ${dosis.indicaciones}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }

            if (dosis.tomaGuardada?.nota?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nota: ${dosis.tomaGuardada.nota}",
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.Gray
                )
            }

            if (dosis.estadoCalculado != EstadoDosis.CONFIRMADA && dosis.estadoCalculado != EstadoDosis.OMITIDA) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(gradientBrush)
                            .clickable { onConfirmar() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirmar toma", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    OutlinedButton(
                        onClick = onOmitir,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Omitir", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TabMedicamentosContent(
    state: MedicacionUiState,
    onEditar: (Medicamento) -> Unit,
    onDesactivar: (String) -> Unit
) {
    val isAdminOrColab = state.currentUserRole == Role.ADMINISTRADOR_FAMILIAR || state.currentUserRole == Role.COLABORADOR
    val medicamentosActivos = state.medicamentos.filter { it.activo }

    if (medicamentosActivos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Aún no hay medicamentos registrados", color = TextoSecundarioColor, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(medicamentosActivos) { med ->
                MedicamentoCard(
                    medicamento = med,
                    puedeEditar = isAdminOrColab,
                    onEditar = { onEditar(med) },
                    onDesactivar = { onDesactivar(med.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun MedicamentoCard(
    medicamento: Medicamento,
    puedeEditar: Boolean,
    onEditar: () -> Unit,
    onDesactivar: () -> Unit
) {
    var showConfirmDesactivar by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (medicamento.activo) LavandaPrimary.copy(alpha = 0.3f) else Color.LightGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = medicamento.nombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (medicamento.activo) TextoPrincipalColor else Color.Gray
                )

                Surface(
                    color = if (medicamento.activo) Color(0xFFE8F5E9) else Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (medicamento.activo) "Activo" else "Desactivado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (medicamento.activo) Color(0xFF2E7D32) else Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Dosis: ${medicamento.dosis}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextoSecundarioColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Horarios: ${medicamento.horarios.joinToString(", ")}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            if (medicamento.indicaciones.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Indicaciones: ${medicamento.indicaciones}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }

            if (puedeEditar && medicamento.activo) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onEditar) {
                        Text("Editar", fontWeight = FontWeight.Bold, color = LavandaPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showConfirmDesactivar = true }) {
                        Text("Desactivar", fontWeight = FontWeight.Bold, color = RojoAlerta)
                    }
                }
            }
        }
    }

    if (showConfirmDesactivar) {
        AlertDialog(
            onDismissRequest = { showConfirmDesactivar = false },
            title = { Text("Desactivar medicamento", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("¿Deseas desactivar ${medicamento.nombre}? Se conservará el historial de tomas pasadas.", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDesactivar = false
                        onDesactivar()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RojoAlerta),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Desactivar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDesactivar = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun TabHistorialContent(state: MedicacionUiState) {
    var filtroMedId by remember { mutableStateOf<String?>(null) }

    val tomasFiltradas = state.tomas.filter { toma ->
        filtroMedId == null || toma.medicamentoId == filtroMedId
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = filtroMedId == null,
                    onClick = { filtroMedId = null },
                    label = { Text("Todos") }
                )
            }
            items(state.medicamentos) { med ->
                FilterChip(
                    selected = filtroMedId == med.id,
                    onClick = { filtroMedId = med.id },
                    label = { Text(med.nombre) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tomasFiltradas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No hay tomas registradas en el historial", color = TextoSecundarioColor, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            val agrupadas = tomasFiltradas.groupBy { it.fecha }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                agrupadas.forEach { (fecha, tomasDia) ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = fecha,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            tomasDia.forEach { toma ->
                                TomaHistorialCard(toma = toma)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun TomaHistorialCard(toma: TomaMedicacion) {
    val esConfirmada = toma.estado == EstadoToma.CONFIRMADA
    val colorBadge = if (esConfirmada) Color(0xFF2E7D32) else Color.Gray
    val horaConf = if (toma.horaConfirmacion > 0L) SimpleDateFormat("HH:mm", Locale.US).format(Date(toma.horaConfirmacion)) else ""

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${toma.horaProgramada} - ${toma.medicamentoNombre}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextoPrincipalColor
                )

                Surface(
                    color = colorBadge.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (esConfirmada) "Confirmada" else "Omitida",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorBadge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Dosis: ${toma.dosis}", fontSize = 14.sp, color = TextoSecundarioColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Registrado por ${toma.confirmadoPorNombre} (${toma.confirmadoPorRol})${if (horaConf.isNotBlank()) " a las $horaConf" else ""}",
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = Color.DarkGray
            )
            if (toma.nota.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Nota: ${toma.nota}", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MedicamentoFormDialog(
    medicamentoInicial: Medicamento?,
    onDismiss: () -> Unit,
    onGuardar: (Medicamento) -> Unit
) {
    var nombre by remember { mutableStateOf(medicamentoInicial?.nombre ?: "") }
    var dosis by remember { mutableStateOf(medicamentoInicial?.dosis ?: "") }
    var indicaciones by remember { mutableStateOf(medicamentoInicial?.indicaciones ?: "") }
    var horarios by remember { mutableStateOf(medicamentoInicial?.horarios ?: listOf("08:00")) }
    var diasSemana by remember { mutableStateOf(medicamentoInicial?.diasSemana ?: emptyList()) }
    var fechaInicio by remember { mutableStateOf(medicamentoInicial?.fechaInicio?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString()) }
    var fechaFin by remember { mutableStateOf(medicamentoInicial?.fechaFin) }

    var showTimePicker by remember { mutableStateOf(false) }
    var tempTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var showFechaInicioDialog by remember { mutableStateOf(false) }
    var showFechaFinDialog by remember { mutableStateOf(false) }

    val diasLabels = listOf("L", "M", "X", "J", "V", "S", "D")
    val diasValues = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (medicamentoInicial == null) "Nuevo Medicamento" else "Editar Medicamento",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del medicamento") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dosis,
                    onValueChange = { dosis = it },
                    label = { Text("Dosis (ej. 50 mg · 1 tableta)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Column {
                    Text("Horarios (24h)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(horarios) { h ->
                            AssistChip(
                                onClick = { horarios = horarios.filter { it != h } },
                                label = { Text(h) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Eliminar", modifier = Modifier.size(16.dp)) }
                            )
                        }
                        item {
                            Button(
                                onClick = { showTimePicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("+ Hora", fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }

                Column {
                    Text("Días de la semana (vacío = todos los días)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        diasLabels.forEachIndexed { index, label ->
                            val dayValue = diasValues[index]
                            val isSelected = diasSemana.contains(dayValue)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    diasSemana = if (isSelected) diasSemana.filter { it != dayValue } else diasSemana + dayValue
                                },
                                label = { Text(label, fontSize = 13.sp) }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showFechaInicioDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Inicio: $fechaInicio", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { showFechaFinDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (fechaFin.isNullOrBlank()) "Sin fin" else "Fin: $fechaFin", fontSize = 13.sp)
                    }
                }

                OutlinedTextField(
                    value = indicaciones,
                    onValueChange = { indicaciones = it },
                    label = { Text("Indicaciones (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (nombre.isNotBlank() && dosis.isNotBlank() && horarios.isNotEmpty()) {
                                onGuardar(
                                    (medicamentoInicial ?: Medicamento()).copy(
                                        nombre = nombre,
                                        dosis = dosis,
                                        horarios = horarios.sorted(),
                                        diasSemana = diasSemana,
                                        fechaInicio = fechaInicio,
                                        fechaFin = fechaFin?.takeIf { it.isNotBlank() },
                                        indicaciones = indicaciones
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Seleccionar Hora", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LavandaPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    WheelTimePicker(horaInicial = tempTime) { time ->
                        tempTime = time
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val timeStr = tempTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                            if (!horarios.contains(timeStr)) {
                                horarios = horarios + timeStr
                            }
                            showTimePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Agregar hora", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showFechaInicioDialog) {
        SelectorFechaDialog(
            fechaInicial = try { LocalDate.parse(fechaInicio) } catch (e: Exception) { LocalDate.now() },
            titulo = "Fecha de inicio",
            onFechaSeleccionada = { fechaInicio = it.toString(); showFechaInicioDialog = false },
            onCancelar = { showFechaInicioDialog = false }
        )
    }

    if (showFechaFinDialog) {
        SelectorFechaDialog(
            fechaInicial = try { LocalDate.parse(fechaFin) } catch (e: Exception) { LocalDate.now() },
            titulo = "Fecha de fin (opcional)",
            onFechaSeleccionada = { fechaFin = it.toString(); showFechaFinDialog = false },
            onCancelar = { showFechaFinDialog = false }
        )
    }
}

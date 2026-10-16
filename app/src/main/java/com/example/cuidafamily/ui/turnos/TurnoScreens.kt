package com.example.cuidafamily.ui.turnos

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.cuidafamily.model.EstadoConfirmacion
import com.example.cuidafamily.model.EstadoTurno
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.Turno
import com.example.cuidafamily.repository.GroupMemberInfo
import com.example.cuidafamily.ui.sos.SosFloatingButton
import com.example.cuidafamily.ui.theme.GradientEnd
import com.example.cuidafamily.ui.theme.GradientStart
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.theme.LavandaPrimaryLight
import com.example.cuidafamily.ui.theme.TextoPrincipalColor
import com.example.cuidafamily.ui.theme.TextoSecundarioColor
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.SelectorFechaDialog
import com.example.cuidafamily.ui.util.UserAvatar
import com.example.cuidafamily.ui.util.WheelTimePicker
import com.example.cuidafamily.ui.util.formatFechaEspanolCorta
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatHora24(horaStr: String): String {
    if (horaStr.isBlank()) return ""
    return try {
        val time = LocalTime.parse(horaStr)
        String.format(Locale.US, "%d:%02d", time.hour, time.minute)
    } catch (e: Exception) {
        horaStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnoScreenContainer(
    viewModel: TurnoViewModel,
    familyGroupId: String,
    userRole: Role,
    userName: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    onSosClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showCrearDialog by remember { mutableStateOf(false) }
    var turnoToEdit by remember { mutableStateOf<Turno?>(null) }
    var showConfigDomicilioDialog by remember { mutableStateOf(false) }
    var turnoToDelete by remember { mutableStateOf<Turno?>(null) }
    var turnoToReassign by remember { mutableStateOf<Turno?>(null) }

    val realUserId = FirebaseAuth.getInstance().currentUser?.uid ?: currentUserId
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var pendingActionOnPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            pendingActionOnPermission?.invoke()
        } else {
            Toast.makeText(context, "Se requiere permiso de ubicación para esta acción.", Toast.LENGTH_SHORT).show()
        }
        pendingActionOnPermission = null
    }

    val requestLocationAndExecute: (() -> Unit) -> Unit = { action ->
        val finePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (finePermission == PackageManager.PERMISSION_GRANTED || coarsePermission == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            pendingActionOnPermission = action
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(realUserId, familyGroupId) {
        viewModel.iniciar(realUserId, familyGroupId)
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.limpiarMensajes()
        }
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.limpiarMensajes()
        }
    }

    val fabGradient = Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
    val esAdmin = userRole == Role.ADMINISTRADOR_FAMILIAR

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = LavandaPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gestión de Turnos", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                    if (esAdmin) {
                        IconButton(onClick = { showConfigDomicilioDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.HomeWork,
                                contentDescription = "Configurar Domicilio",
                                tint = LavandaPrimary
                            )
                        }
                    }
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
                if (esAdmin) {
                    FloatingActionButton(
                        onClick = { showCrearDialog = true },
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .size(56.dp)
                            .background(fabGradient, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Asignar Turno", tint = Color.White)
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
            ) {
                SemanaSelectorBar(
                    offsetSemanas = uiState.offsetSemanas,
                    diasSemana = viewModel.getDiasDeLaSemanaActual(),
                    onAnteriorClick = { viewModel.cambiarSemana(-1) },
                    onSiguienteClick = { viewModel.cambiarSemana(1) },
                    onResetClick = { viewModel.resetearSemana() }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LavandaPrimary)
                        }
                    } else {
                        val diasSemana = viewModel.getDiasDeLaSemanaActual()

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Sección de Avisos de Turnos Cancelados para el Cuidador
                            val avisosCancelados = uiState.turnosCanceladosAvisos
                            if (avisosCancelados.isNotEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(2.dp, Color(0xFFC62828)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Surface(
                                                color = Color(0xFFFFEBEE),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "🔔 Avisos de Turnos Cancelados (${avisosCancelados.size})",
                                                    color = Color(0xFFC62828),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            avisosCancelados.forEach { turno ->
                                                Surface(
                                                    color = Color(0xFFFFF5F5),
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text(
                                                            text = "🗑️ ${turno.avisoParaCuidador.ifBlank { "Turno cancelado por el Administrador" }}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = Color(0xFFC62828)
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "Horario: ${formatHora24(turno.horaInicio)} - ${formatHora24(turno.horaFin)}",
                                                            fontSize = 13.sp,
                                                            color = TextoSecundarioColor
                                                        )
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Button(
                                                            onClick = { viewModel.borrarTurnoDefinitivo(familyGroupId, turno.id) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                                            shape = RoundedCornerShape(10.dp),
                                                            modifier = Modifier.height(36.dp)
                                                        ) {
                                                            Text("Entendido", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Sección de Respuestas Recientes para Administrador
                            if (esAdmin) {
                                val respuestasNuevas = uiState.turnosActivos.filter { !it.respuestaVistaPorAdmin }
                                if (respuestasNuevas.isNotEmpty()) {
                                    item {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(2.dp, LavandaPrimary.copy(alpha = 0.3f)),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(18.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Surface(
                                                        color = LavandaPrimaryLight,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "💬 Respuestas recientes (${respuestasNuevas.size})",
                                                            color = LavandaPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.weight(1f))

                                                    TextButton(onClick = { viewModel.marcarTodasRespuestasVistas(familyGroupId) }) {
                                                        Text("Marcar todas vistas", fontSize = 12.sp, color = LavandaPrimary, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                respuestasNuevas.forEach { turno ->
                                                    TarjetaRespuestaRecienteAdmin(
                                                        turno = turno,
                                                        onMarcarVisto = {
                                                            viewModel.marcarRespuestaVista(familyGroupId, turno.id)
                                                        },
                                                        onReasignar = {
                                                            turnoToReassign = turno
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Sección "Pendientes de tu respuesta" para el Cuidador (excluye turnos cuya fechaFin ya pasó)
                            val turnosPendientes = uiState.turnosActivos.filter {
                                it.cuidadorUserId == realUserId && it.confirmacion == EstadoConfirmacion.PENDIENTE && !it.esVencido()
                            }
                            if (turnosPendientes.isNotEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(2.dp, Color(0xFFE65100)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Surface(
                                                color = Color(0xFFFFF3E0),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "⏳ Pendientes de tu respuesta (${turnosPendientes.size})",
                                                    color = Color(0xFFE65100),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            turnosPendientes.forEach { turno ->
                                                TarjetaTurnoPendienteRespuesta(
                                                    turno = turno,
                                                    onConfirmar = {
                                                        viewModel.responderConfirmacionTurno(familyGroupId, turno.id, EstadoConfirmacion.CONFIRMADO, "")
                                                    },
                                                    onRechazar = { motivo ->
                                                        viewModel.responderConfirmacionTurno(familyGroupId, turno.id, EstadoConfirmacion.RECHAZADO, motivo)
                                                    },
                                                    onSolicitarReemplazo = { motivo ->
                                                        viewModel.responderConfirmacionTurno(familyGroupId, turno.id, EstadoConfirmacion.REEMPLAZO_SOLICITADO, motivo)
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Banner "Turnos registrados fuera de esta semana" si existen turnos pero ninguno aplica a la semana visible
                            val turnosAplicanSemana = diasSemana.sumOf { dia -> viewModel.obtenerTurnosParaDia(dia).size }
                            if (uiState.turnosActivos.isNotEmpty() && turnosAplicanSemana == 0) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                text = "📋 Turnos registrados en el grupo (${uiState.turnosActivos.size})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextoPrincipalColor
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Hay turnos registrados cuyo rango de fechas es anterior o posterior a la semana visible. Usa las flechas superiores para navegar de semana.",
                                                fontSize = 12.sp,
                                                color = TextoSecundarioColor
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                BannerDestacadoTurno(
                                    userRole = userRole,
                                    currentUserId = realUserId,
                                    viewModel = viewModel,
                                    familyGroupId = familyGroupId,
                                    onRequestLocation = requestLocationAndExecute,
                                    fusedLocationClient = locationClient
                                )
                            }

                            items(diasSemana, key = { it.fechaStr }) { diaItem ->
                                val turnosDelDia = viewModel.obtenerTurnosParaDia(diaItem)

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${diaItem.nombreDia} ${diaItem.numeroDiaMes}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (diaItem.esHoy) LavandaPrimary else TextoPrincipalColor
                                        )

                                        if (diaItem.esHoy) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = LavandaPrimary,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "HOY",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (turnosDelDia.isEmpty()) {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Sin turnos asignados",
                                                fontSize = 13.sp,
                                                color = TextoSecundarioColor,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    } else {
                                        turnosDelDia.forEach { itemConEstado ->
                                            TurnoCard(
                                                turnoConEstado = itemConEstado,
                                                currentUserId = realUserId,
                                                userRole = userRole,
                                                canDelete = esAdmin,
                                                onEditClick = { turnoToEdit = itemConEstado.turno },
                                                onDeleteClick = { turnoToDelete = itemConEstado.turno },
                                                onReasignarClick = { turnoToReassign = itemConEstado.turno },
                                                onCallClick = { tel ->
                                                    try {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "No se pudo realizar la llamada", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onResponderConfirmacion = { estado, motivo ->
                                                    viewModel.responderConfirmacionTurno(familyGroupId, itemConEstado.turno.id, estado, motivo)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showCrearDialog || turnoToEdit != null) {
        CrearTurnoDialog(
            miembros = uiState.miembrosGrupo,
            initialTurno = turnoToEdit,
            isSaving = uiState.isSaving,
            onDismiss = {
                showCrearDialog = false
                turnoToEdit = null
            },
            onCrear = { cuidadorUserId, cuidadorNombre, tel, dias, hIni, hFin, tareas, fInicio, fFin ->
                if (turnoToEdit != null) {
                    val nuevoTurnoEditado = turnoToEdit!!.copy(
                        cuidadorUserId = cuidadorUserId,
                        cuidadorNombre = cuidadorNombre,
                        cuidadorTelefono = tel,
                        diasSemana = dias,
                        horaInicio = hIni,
                        horaFin = hFin,
                        tareas = tareas,
                        fechaInicio = fInicio,
                        fechaFin = fFin
                    )
                    viewModel.modificarTurno(familyGroupId, turnoToEdit, nuevoTurnoEditado)
                    turnoToEdit = null
                } else {
                    viewModel.crearTurno(
                        familyGroupId = familyGroupId,
                        cuidadorUserId = cuidadorUserId,
                        cuidadorNombre = cuidadorNombre,
                        cuidadorTelefono = tel,
                        diasSemana = dias,
                        horaInicio = hIni,
                        horaFin = hFin,
                        tareas = tareas,
                        creadoPorUserId = realUserId,
                        fechaInicio = fInicio,
                        fechaFin = fFin
                    )
                    showCrearDialog = false
                }
            }
        )
    }

    if (showConfigDomicilioDialog) {
        ConfigurarDomicilioDialog(
            currentLat = uiState.domicilioLat,
            currentLng = uiState.domicilioLng,
            isSaving = uiState.isSaving,
            onDismiss = { showConfigDomicilioDialog = false },
            onRequestLocation = requestLocationAndExecute,
            fusedLocationClient = locationClient,
            onGuardar = { lat, lng ->
                viewModel.configurarDomicilio(familyGroupId, lat, lng)
                showConfigDomicilioDialog = false
            }
        )
    }

    turnoToReassign?.let { t ->
        ReasignarTurnoDialog(
            miembros = uiState.miembrosGrupo,
            isSaving = uiState.isSaving,
            onDismiss = { turnoToReassign = null },
            onConfirm = { m ->
                viewModel.reasignarTurno(familyGroupId, t.id, m.member.userId, m.userName, "")
                turnoToReassign = null
            }
        )
    }

    turnoToDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { turnoToDelete = null },
            title = { Text("Eliminar Turno", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("¿Eliminar este turno asignado a '${t.cuidadorNombre}'? Esta acción desactivará el turno del calendario.", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.desactivarTurno(familyGroupId, t.id)
                        turnoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { turnoToDelete = null }, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TarjetaRespuestaRecienteAdmin(
    turno: Turno,
    onMarcarVisto: () -> Unit,
    onReasignar: () -> Unit
) {
    val (badgeBg, badgeText, badgeLabel) = when (turno.confirmacion) {
        EstadoConfirmacion.CONFIRMADO -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "✅ Confirmó")
        EstadoConfirmacion.RECHAZADO -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "❌ No puede cubrirlo")
        EstadoConfirmacion.REEMPLAZO_SOLICITADO -> Triple(Color(0xFFEDE7F6), Color(0xFF673AB7), "🔄 Solicita reemplazo")
        EstadoConfirmacion.PENDIENTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "⏳ Pendiente")
    }

    val diasTexto = turno.diasSemana.map { dayNum ->
        when (dayNum) {
            2 -> "L"; 3 -> "M"; 4 -> "X"; 5 -> "J"; 6 -> "V"; 7 -> "S"; 1 -> "D"; else -> ""
        }
    }.filter { it.isNotBlank() }.joinToString(", ")

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.2f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = turno.cuidadorNombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = TextoPrincipalColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = badgeLabel,
                        color = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Horario: $diasTexto · ${formatHora24(turno.horaInicio)} - ${formatHora24(turno.horaFin)}",
                fontSize = 14.sp,
                color = TextoSecundarioColor
            )

            if (turno.motivoRechazo.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Motivo: \"${turno.motivoRechazo}\"",
                    fontSize = 13.sp,
                    color = if (turno.confirmacion == EstadoConfirmacion.RECHAZADO) Color(0xFFC62828) else Color(0xFF673AB7),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (turno.confirmacion == EstadoConfirmacion.RECHAZADO || turno.confirmacion == EstadoConfirmacion.REEMPLAZO_SOLICITADO) {
                    Button(
                        onClick = onReasignar,
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Reasignar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = onMarcarVisto,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Marcar como visto", fontSize = 12.sp, color = TextoSecundarioColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TarjetaTurnoPendienteRespuesta(
    turno: Turno,
    onConfirmar: () -> Unit,
    onRechazar: (String) -> Unit,
    onSolicitarReemplazo: (String) -> Unit
) {
    var showRechazoDialog by remember { mutableStateOf(false) }
    var showReemplazoDialog by remember { mutableStateOf(false) }

    val diasTexto = turno.diasSemana.map { dayNum ->
        when (dayNum) {
            2 -> "Lun"
            3 -> "Mar"
            4 -> "Mié"
            5 -> "Jue"
            6 -> "Vie"
            7 -> "Sáb"
            1 -> "Dom"
            else -> ""
        }
    }.joinToString(", ")

    Surface(
        color = Color(0xFFFFF8E1),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFFE082)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Horario: ${formatHora24(turno.horaInicio)} - ${formatHora24(turno.horaFin)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text("Días: $diasTexto", fontSize = 13.sp, color = TextoSecundarioColor, fontWeight = FontWeight.SemiBold)
            }

            if (turno.tareas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    turno.tareas.forEach { t ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(t, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirmar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Puedo cubrir este turno", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = { showRechazoDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                    border = BorderStroke(1.5.dp, Color(0xFFC62828)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("No puedo cubrirlo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showReemplazoDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF673AB7)),
                    border = BorderStroke(1.5.dp, Color(0xFF673AB7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Solicitar reemplazo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showRechazoDialog) {
        MotivoRechazoTurnoDialog(
            onDismiss = { showRechazoDialog = false },
            onConfirm = { motivo ->
                onRechazar(motivo)
                showRechazoDialog = false
            }
        )
    }

    if (showReemplazoDialog) {
        MotivoRechazoTurnoDialog(
            titulo = "Solicitar reemplazo",
            subtitulo = "Explica la razón por la que requieres un reemplazo:",
            onDismiss = { showReemplazoDialog = false },
            onConfirm = { motivo ->
                onSolicitarReemplazo(motivo)
                showReemplazoDialog = false
            }
        )
    }
}

@Composable
fun TurnoCard(
    turnoConEstado: TurnoConEstado,
    currentUserId: String = "",
    userRole: Role = Role.CUIDADOR_EXTERNO,
    canDelete: Boolean,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onReasignarClick: () -> Unit = {},
    onCallClick: (String) -> Unit,
    onResponderConfirmacion: (EstadoConfirmacion, String) -> Unit = { _, _ -> }
) {
    val turno = turnoConEstado.turno
    val showButtons = (turno.cuidadorUserId == currentUserId && turno.confirmacion == EstadoConfirmacion.PENDIENTE)

    Log.d("TurnoCard", "turnoId=${turno.id}, cuidadorUserId=${turno.cuidadorUserId}, currentUserId=$currentUserId, confirmacion=${turno.confirmacion}, rol=$userRole, showButtons=$showButtons")

    val (badgeBg, badgeText, badgeLabel) = when (turnoConEstado.estado) {
        EstadoTurno.PROGRAMADO -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Programado")
        EstadoTurno.EN_CURSO -> Triple(Color(0xFFEDE7F6), Color(0xFF673AB7), "En Curso")
        EstadoTurno.COMPLETADO -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Completado")
    }

    var showMotivoDialog by remember { mutableStateOf(false) }
    val esAdmin = userRole == Role.ADMINISTRADOR_FAMILIAR

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (turno.avisoParaCuidador.isNotBlank() && turno.activo && turno.cuidadorUserId == currentUserId) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFE082)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "✏️ Turno modificado por el Administrador",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = turno.avisoParaCuidador,
                            fontSize = 13.sp,
                            color = TextoPrincipalColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        color = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                if (turno.fechaInicio == 0L) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)) {
                        Text("Sin rango de fechas", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (turnoConEstado.checkIn != null) {
                    val iconCheck = if (turnoConEstado.checkIn.ubicacionValida) "✅ Entrada GPS OK" else "⚠️ Fuera de rango"
                    Text(iconCheck, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (turnoConEstado.checkIn.ubicacionValida) Color(0xFF2E7D32) else Color(0xFFE65100))
                }

                if (esAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onEditClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("✏️ Editar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary)
                        }

                        OutlinedButton(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                            border = BorderStroke(1.dp, Color(0xFFC62828)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("🗑️ Eliminar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = turno.cuidadorNombre,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = TextoPrincipalColor
                    )
                    Text(
                        text = "Horario: ${formatHora24(turno.horaInicio)} - ${formatHora24(turno.horaFin)}",
                        fontSize = 14.sp,
                        color = TextoSecundarioColor
                    )
                }

                if (turno.cuidadorTelefono.isNotBlank()) {
                    IconButton(onClick = { onCallClick(turno.cuidadorTelefono) }) {
                        Icon(Icons.Default.Call, contentDescription = "Llamar", tint = Color(0xFF2E7D32))
                    }
                }
            }

            // Badges y botones de confirmación de asistencia
            if (showButtons) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onResponderConfirmacion(EstadoConfirmacion.CONFIRMADO, "") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Puedo cubrir este turno", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { showMotivoDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        border = BorderStroke(1.5.dp, Color(0xFFC62828)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("No puedo cubrirlo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                when (turno.confirmacion) {
                    EstadoConfirmacion.CONFIRMADO -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                            Text("✅ Confirmado", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                        }
                    }
                    EstadoConfirmacion.RECHAZADO -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)) {
                                    Text("❌ Rechazado", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    if (turno.motivoRechazo.isNotBlank()) {
                                        Text("Motivo: \"${turno.motivoRechazo}\"", color = Color(0xFFC62828).copy(alpha = 0.8f), fontSize = 11.sp)
                                    }
                                }
                            }
                            if (esAdmin) {
                                Spacer(modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = onReasignarClick,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Reasignar", fontSize = 12.sp, color = LavandaPrimary)
                                }
                            }
                        }
                    }
                    EstadoConfirmacion.REEMPLAZO_SOLICITADO -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color(0xFFEDE7F6), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)) {
                                    Text("🔄 Solicita reemplazo", color = Color(0xFF673AB7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    if (turno.motivoRechazo.isNotBlank()) {
                                        Text("Motivo: \"${turno.motivoRechazo}\"", color = Color(0xFF673AB7).copy(alpha = 0.8f), fontSize = 11.sp)
                                    }
                                }
                            }
                            if (esAdmin) {
                                Spacer(modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = onReasignarClick,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Reasignar", fontSize = 12.sp, color = LavandaPrimary)
                                }
                            }
                        }
                    }
                    EstadoConfirmacion.PENDIENTE -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(8.dp)) {
                            Text("⏳ Pendiente", color = Color(0xFFE65100), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            if (turno.tareas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    turno.tareas.forEach { tarea ->
                        Surface(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = tarea,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoPrincipalColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMotivoDialog) {
        MotivoRechazoTurnoDialog(
            onDismiss = { showMotivoDialog = false },
            onConfirm = { motivo ->
                onResponderConfirmacion(EstadoConfirmacion.RECHAZADO, motivo)
                showMotivoDialog = false
            }
        )
    }
}

@Composable
fun MotivoRechazoTurnoDialog(
    titulo: String = "No puedo cubrir el turno",
    subtitulo: String = "Indica un motivo breve para informar al Administrador del grupo (opcional):",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivoText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = subtitulo,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                OutlinedTextField(
                    value = motivoText,
                    onValueChange = { motivoText = it },
                    label = { Text("Motivo / Explicación") },
                    placeholder = { Text("Ej: Inconveniente personal / Conflicto de horario") },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReasignarTurnoDialog(
    miembros: List<GroupMemberInfo>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (GroupMemberInfo) -> Unit
) {
    var selectedMiembro by remember { mutableStateOf<GroupMemberInfo?>(miembros.firstOrNull { it.member.role == Role.CUIDADOR_EXTERNO } ?: miembros.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = LavandaPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reasignar Turno", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Selecciona el nuevo Cuidador Externo que asumirá este turno:",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    miembros.forEach { m ->
                        val isSelected = selectedMiembro?.member?.userId == m.member.userId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMiembro = m },
                            label = { Text(m.userName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LavandaPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedMiembro?.let { onConfirm(it) } },
                enabled = !isSaving && selectedMiembro != null,
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Reasignar Turno", fontWeight = FontWeight.Bold)
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

@Composable
fun SemanaSelectorBar(
    offsetSemanas: Int,
    diasSemana: List<DiaSemanaItem>,
    onAnteriorClick: () -> Unit,
    onSiguienteClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val primerDia = diasSemana.firstOrNull()?.numeroDiaMes ?: 1
    val ultimoDia = diasSemana.lastOrNull()?.numeroDiaMes ?: 31
    val mesNombre = SimpleDateFormat("MMM", Locale("es", "ES")).format(Calendar.getInstance().apply {
        add(Calendar.WEEK_OF_YEAR, offsetSemanas)
    }.time).replaceFirstChar { it.uppercase() }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAnteriorClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Semana Anterior", tint = LavandaPrimary)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { if (offsetSemanas != 0) onResetClick() }
            ) {
                Text(
                    text = "Semana $primerDia - $ultimoDia $mesNombre",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (offsetSemanas != 0) {
                    Text(
                        text = "Volver a esta semana",
                        fontSize = 10.sp,
                        color = LavandaPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(onClick = onSiguienteClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Semana Siguiente", tint = LavandaPrimary)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BannerDestacadoTurno(
    userRole: Role,
    currentUserId: String,
    viewModel: TurnoViewModel,
    familyGroupId: String,
    onRequestLocation: ((() -> Unit) -> Unit),
    fusedLocationClient: FusedLocationProviderClient
) {
    val context = LocalContext.current
    val diasSemana = viewModel.getDiasDeLaSemanaActual()
    val hoyItem = diasSemana.firstOrNull { it.esHoy } ?: return
    val turnosHoy = viewModel.obtenerTurnosParaDia(hoyItem)

    if (userRole == Role.CUIDADOR_EXTERNO) {
        val miTurnoHoy = turnosHoy.firstOrNull { it.turno.cuidadorUserId == currentUserId } ?: return

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, Color(0xFF7C4DFF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF7C4DFF),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "● Turno en Curso",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${formatHora24(miTurnoHoy.turno.horaInicio)} - ${formatHora24(miTurnoHoy.turno.horaFin)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color(0xFF7C4DFF)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Hola, ${miTurnoHoy.turno.cuidadorNombre}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = TextoPrincipalColor
                )

                if (miTurnoHoy.turno.tareas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tareas asignadas:", fontSize = 13.sp, color = TextoSecundarioColor, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        miTurnoHoy.turno.tareas.forEach { t ->
                            Surface(
                                color = LavandaPrimaryLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(t, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val checkIn = miTurnoHoy.checkIn
                if (checkIn == null) {
                    Button(
                        onClick = {
                            onRequestLocation {
                                try {
                                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                        .addOnSuccessListener { loc ->
                                            if (loc != null) {
                                                viewModel.registrarCheckIn(familyGroupId, miTurnoHoy.turno.id, currentUserId, loc.latitude, loc.longitude)
                                            } else {
                                                Toast.makeText(context, "No se pudo obtener la ubicación GPS.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al consultar GPS.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Marcar Llegada (GPS)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    if (checkIn.ubicacionValida) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("✅ Entrada Validada por Ubicación", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                                    Text("Hora check-in: ${checkIn.horaCheckIn}", fontSize = 12.sp, color = TextoSecundarioColor)
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFE082)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE65100))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("⚠️ Entrada Registrada fuera de Rango", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE65100))
                                    Text("Hora check-in: ${checkIn.horaCheckIn} (Ubicación a >150m del domicilio)", fontSize = 12.sp, color = TextoSecundarioColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        val turnoEnCurso = turnosHoy.firstOrNull { it.estado == EstadoTurno.EN_CURSO } ?: return

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(LavandaPrimaryLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = LavandaPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Turno en curso hoy", fontSize = 12.sp, color = TextoSecundarioColor, fontWeight = FontWeight.Bold)
                    Text(turnoEnCurso.turno.cuidadorNombre, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextoPrincipalColor)
                    Text("${formatHora24(turnoEnCurso.turno.horaInicio)} - ${formatHora24(turnoEnCurso.turno.horaFin)}", fontSize = 13.sp, color = LavandaPrimary)
                }

                if (turnoEnCurso.turno.cuidadorTelefono.isNotBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${turnoEnCurso.turno.cuidadorTelefono}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo realizar la llamada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Llamar", tint = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrearTurnoDialog(
    miembros: List<GroupMemberInfo>,
    initialTurno: Turno? = null,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCrear: (String, String, String, List<Int>, String, String, List<String>, Long, Long?) -> Unit
) {
    val context = LocalContext.current
    var selectedMiembro by remember {
        mutableStateOf(
            miembros.firstOrNull { it.member.userId == initialTurno?.cuidadorUserId }
                ?: miembros.firstOrNull { it.member.role == Role.CUIDADOR_EXTERNO }
                ?: miembros.firstOrNull()
        )
    }
    var telefonoText by remember { mutableStateOf(initialTurno?.cuidadorTelefono ?: "") }

    val diasOpciones = listOf(
        Pair("L", 2),
        Pair("M", 3),
        Pair("X", 4),
        Pair("J", 5),
        Pair("V", 6),
        Pair("S", 7),
        Pair("D", 1)
    )

    var selectedDias by remember {
        mutableStateOf(initialTurno?.diasSemana?.toSet() ?: setOf(2, 3, 4, 5, 6))
    }

    var presetSeleccionado by remember {
        mutableStateOf(
            when {
                initialTurno?.horaInicio == "9:00" && initialTurno.horaFin == "14:00" -> "MANANA"
                initialTurno?.horaInicio == "15:00" && initialTurno.horaFin == "20:00" -> "TARDE"
                initialTurno?.horaInicio == "20:00" && initialTurno.horaFin == "8:00" -> "NOCHE"
                initialTurno != null -> "CUSTOM"
                else -> "MANANA"
            }
        )
    }

    var horaInicioCustom by remember {
        mutableStateOf(
            try { LocalTime.parse(initialTurno?.horaInicio) } catch (e: Exception) { LocalTime.of(9, 0) }
        )
    }
    var horaFinCustom by remember {
        mutableStateOf(
            try { LocalTime.parse(initialTurno?.horaFin) } catch (e: Exception) { LocalTime.of(14, 0) }
        )
    }

    val tareasOpciones = listOf("Medicación", "Paseo", "Almuerzo", "Higiene", "Cita Médica", "Presión")
    var selectedTareas by remember {
        mutableStateOf(initialTurno?.tareas?.toSet() ?: setOf("Medicación", "Almuerzo"))
    }

    var fechaInicioLocalDate by remember {
        mutableStateOf(
            if (initialTurno != null && initialTurno.fechaInicio > 0L) {
                Instant.ofEpochMilli(initialTurno.fechaInicio).atZone(ZoneOffset.UTC).toLocalDate()
            } else {
                LocalDate.now()
            }
        )
    }

    var fechaFinLocalDate by remember {
        mutableStateOf<LocalDate?>(
            if (initialTurno?.fechaFin != null && initialTurno.fechaFin > 0L) {
                Instant.ofEpochMilli(initialTurno.fechaFin).atZone(ZoneOffset.UTC).toLocalDate()
            } else {
                null
            }
        )
    }

    var sinFechaFin by remember {
        mutableStateOf(initialTurno == null || initialTurno.fechaFin == null || initialTurno.fechaFin == 0L)
    }

    var showDesdeDatePicker by remember { mutableStateOf(false) }
    var showHastaDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = LavandaPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTurno != null) "Editar Turno" else "Asignar Nuevo Turno",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Selecciona el Cuidador:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        miembros.forEach { m ->
                            val isSelected = selectedMiembro?.member?.userId == m.member.userId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMiembro = m },
                                label = { Text(m.userName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LavandaPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = telefonoText,
                    onValueChange = { telefonoText = it },
                    label = { Text("Teléfono de Contacto (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Rango de Fechas (Desde - Hasta)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rango de Vigencia del Turno:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                val hoy = LocalDate.now()
                                fechaInicioLocalDate = hoy
                                fechaFinLocalDate = hoy
                                sinFechaFin = false

                                val calendarDayOfWeek = when (hoy.dayOfWeek) {
                                    DayOfWeek.SUNDAY -> 1
                                    DayOfWeek.MONDAY -> 2
                                    DayOfWeek.TUESDAY -> 3
                                    DayOfWeek.WEDNESDAY -> 4
                                    DayOfWeek.THURSDAY -> 5
                                    DayOfWeek.FRIDAY -> 6
                                    DayOfWeek.SATURDAY -> 7
                                }
                                selectedDias = setOf(calendarDayOfWeek)
                            }
                        ) {
                            Text("Solo este día", fontSize = 11.sp, color = LavandaPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = formatFechaEspanolCorta(fechaInicioLocalDate),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Desde") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDesdeDatePicker = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color.Gray,
                                disabledTextColor = TextoPrincipalColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (!sinFechaFin) {
                            OutlinedTextField(
                                value = fechaFinLocalDate?.let { formatFechaEspanolCorta(it) } ?: "Elegir fecha",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Hasta") },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showHastaDatePicker = true },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = Color.Gray,
                                    disabledTextColor = TextoPrincipalColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Repetir indefinidamente (sin fecha final)", fontSize = 12.sp, color = TextoSecundarioColor)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = sinFechaFin,
                            onCheckedChange = { isChecked ->
                                sinFechaFin = isChecked
                                if (isChecked) {
                                    fechaFinLocalDate = null
                                } else if (fechaFinLocalDate == null) {
                                    fechaFinLocalDate = fechaInicioLocalDate
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = LavandaPrimary)
                        )
                    }
                }

                Column {
                    Text("Días de la semana:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        diasOpciones.forEach { (letra, dayNum) ->
                            val isSel = selectedDias.contains(dayNum)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) LavandaPrimary else Color(0xFFF5F5F5))
                                    .clickable {
                                        selectedDias = if (isSel) selectedDias - dayNum else selectedDias + dayNum
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(letra, color = if (isSel) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Column {
                    Text("Horario:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = presetSeleccionado == "MANANA",
                            onClick = { presetSeleccionado = "MANANA" },
                            label = { Text("☀️ Mañana (9:00 - 14:00)", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = presetSeleccionado == "TARDE",
                            onClick = { presetSeleccionado = "TARDE" },
                            label = { Text("🌇 Tarde (15:00 - 20:00)", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = presetSeleccionado == "NOCHE",
                            onClick = { presetSeleccionado = "NOCHE" },
                            label = { Text("🌙 Noche (20:00 - 8:00)", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = presetSeleccionado == "CUSTOM",
                            onClick = { presetSeleccionado = "CUSTOM" },
                            label = { Text("Personalizado", fontSize = 12.sp) }
                        )
                    }

                    if (presetSeleccionado == "CUSTOM") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Hora de Inicio:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        WheelTimePicker(horaInicial = horaInicioCustom, onTimeChanged = { horaInicioCustom = it })
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Hora de Término:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        WheelTimePicker(horaInicial = horaFinCustom, onTimeChanged = { horaFinCustom = it })
                    }
                }

                Column {
                    Text("Tareas del Turno:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tareasOpciones.forEach { tarea ->
                            val isSel = selectedTareas.contains(tarea)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedTareas = if (isSel) selectedTareas - tarea else selectedTareas + tarea
                                },
                                label = { Text(tarea, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDias.isEmpty()) {
                        Toast.makeText(context, "Selecciona al menos un día de la semana", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (!sinFechaFin && fechaFinLocalDate != null) {
                        if (fechaFinLocalDate!!.isBefore(fechaInicioLocalDate)) {
                            Toast.makeText(context, "La fecha de fin debe ser mayor o igual a la de inicio", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }

                    val (hIniStr, hFinStr) = when (presetSeleccionado) {
                        "MANANA" -> Pair("9:00", "14:00")
                        "TARDE" -> Pair("15:00", "20:00")
                        "NOCHE" -> Pair("20:00", "8:00")
                        else -> Pair(
                            String.format(Locale.US, "%d:%02d", horaInicioCustom.hour, horaInicioCustom.minute),
                            String.format(Locale.US, "%d:%02d", horaFinCustom.hour, horaFinCustom.minute)
                        )
                    }

                    val fInicioMs = fechaInicioLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    val fFinMs = if (sinFechaFin || fechaFinLocalDate == null) null else fechaFinLocalDate!!.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

                    selectedMiembro?.let { m ->
                        onCrear(
                            m.member.userId,
                            m.userName,
                            telefonoText,
                            selectedDias.toList(),
                            hIniStr,
                            hFinStr,
                            selectedTareas.toList(),
                            fInicioMs,
                            fFinMs
                        )
                    }
                },
                enabled = !isSaving && selectedMiembro != null && selectedDias.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (initialTurno != null) "Guardar Cambios" else "Asignar Turno", fontWeight = FontWeight.Bold)
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

    if (showDesdeDatePicker) {
        SelectorFechaDialog(
            fechaInicial = fechaInicioLocalDate,
            titulo = "Seleccionar fecha de inicio",
            onFechaSeleccionada = { nuevaFecha ->
                fechaInicioLocalDate = nuevaFecha
                if (!sinFechaFin && (fechaFinLocalDate == null || fechaFinLocalDate!!.isBefore(nuevaFecha))) {
                    fechaFinLocalDate = nuevaFecha
                }
                showDesdeDatePicker = false
            },
            onCancelar = { showDesdeDatePicker = false }
        )
    }

    if (showHastaDatePicker) {
        SelectorFechaDialog(
            fechaInicial = fechaFinLocalDate ?: fechaInicioLocalDate,
            titulo = "Seleccionar fecha de término",
            fechaMinima = fechaInicioLocalDate,
            onFechaSeleccionada = { nuevaFecha ->
                fechaFinLocalDate = nuevaFecha
                showHastaDatePicker = false
            },
            onCancelar = { showHastaDatePicker = false }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ConfigurarDomicilioDialog(
    currentLat: Double?,
    currentLng: Double?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onRequestLocation: ((() -> Unit) -> Unit),
    fusedLocationClient: FusedLocationProviderClient,
    onGuardar: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var latCaptured by remember { mutableStateOf(currentLat) }
    var lngCaptured by remember { mutableStateOf(currentLng) }
    var isCapturing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HomeWork, contentDescription = null, tint = LavandaPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configurar Domicilio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Captura la ubicación GPS de referencia de la casa del adulto mayor. Esta posición se utilizará para validar la llegada del cuidador (<150m).",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Surface(
                    color = LavandaPrimaryLight,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (latCaptured != null && lngCaptured != null) {
                            Text("Ubicación Referencia GPS:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Lat: $latCaptured", fontSize = 12.sp, color = Color.Gray)
                            Text("Lng: $lngCaptured", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            Text("Sin ubicación de domicilio registrada", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Button(
                    onClick = {
                        isCapturing = true
                        onRequestLocation {
                            try {
                                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                    .addOnSuccessListener { loc ->
                                        isCapturing = false
                                        if (loc != null) {
                                            latCaptured = loc.latitude
                                            lngCaptured = loc.longitude
                                        } else {
                                            Toast.makeText(context, "No se pudo obtener la posición GPS actual.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        isCapturing = false
                                        Toast.makeText(context, "Error leyendo GPS.", Toast.LENGTH_SHORT).show()
                                    }
                            } catch (e: Exception) {
                                isCapturing = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capturar GPS de Casa Ahora", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (latCaptured != null && lngCaptured != null) {
                        onGuardar(latCaptured!!, lngCaptured!!)
                    }
                },
                enabled = !isSaving && latCaptured != null && lngCaptured != null,
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Guardar Domicilio", fontWeight = FontWeight.Bold)
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

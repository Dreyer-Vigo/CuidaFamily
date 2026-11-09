package com.example.cuidafamily.ui.turnos

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.cuidafamily.util.formatearSoles
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
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
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.cuidafamily.BuildConfig
import com.example.cuidafamily.model.EstadoConfirmacion
import com.example.cuidafamily.model.EstadoSolicitud
import com.example.cuidafamily.model.EstadoTurno
import com.example.cuidafamily.model.ExcepcionTurno
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.SolicitudIntercambio
import com.example.cuidafamily.model.TipoIntercambio
import com.example.cuidafamily.model.Turno
import com.example.cuidafamily.model.cuidadorEnFecha
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
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    var solicitudToAprobar by remember { mutableStateOf<SolicitudIntercambio?>(null) }
    var solicitudToRechazar by remember { mutableStateOf<SolicitudIntercambio?>(null) }
    var turnoParaSolicitarCambio by remember { mutableStateOf<Pair<Turno, String>?>(null) }

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
                            // 1. SECCIÓN: SOLICITUDES DE INTERCAMBIO PENDIENTES (Solo Administrador)
                            val solicitudesPendientesAdmin = if (esAdmin) {
                                uiState.solicitudesIntercambio.filter { it.estado == EstadoSolicitud.PENDIENTE }
                            } else emptyList()

                            if (solicitudesPendientesAdmin.isNotEmpty()) {
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
                                                    text = "🔄 Solicitudes de Intercambio Pendientes (${solicitudesPendientesAdmin.size})",
                                                    color = Color(0xFFE65100),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            solicitudesPendientesAdmin.forEach { sol ->
                                                TarjetaSolicitudAdmin(
                                                    solicitud = sol,
                                                    onAprobar = { solicitudToAprobar = sol },
                                                    onRechazar = { solicitudToRechazar = sol }
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. SECCIÓN: REEMPLAZOS ASIGNADOS PENDIENTES (Solo para el Cuidador Reemplazo)
                            val reemplazosPendientes = if (!esAdmin) {
                                uiState.excepcionesTurno.filter { it.cuidadorReemplazoUserId == realUserId && it.confirmacion == EstadoConfirmacion.PENDIENTE }
                            } else emptyList()

                            if (reemplazosPendientes.isNotEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(2.dp, LavandaPrimary),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Surface(
                                                color = LavandaPrimaryLight,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "📅 Reemplazos de Turno Asignados (${reemplazosPendientes.size})",
                                                    color = LavandaPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            reemplazosPendientes.forEach { exc ->
                                                TarjetaReemplazoPendiente(
                                                    excepcion = exc,
                                                    onAceptar = {
                                                        viewModel.responderExcepcionTurno(familyGroupId, exc.id, EstadoConfirmacion.CONFIRMADO)
                                                    },
                                                    onRechazar = {
                                                        viewModel.responderExcepcionTurno(familyGroupId, exc.id, EstadoConfirmacion.RECHAZADO)
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. SECCIÓN: MIS SOLICITUDES ENVIADAS (Para el Cuidador Solicitante)
                            val misSolicitudes = if (!esAdmin) {
                                uiState.solicitudesIntercambio.filter { it.solicitanteUserId == realUserId }
                            } else emptyList()

                            if (misSolicitudes.isNotEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.3f)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                text = "📋 Mis Solicitudes de Cambio (${misSolicitudes.size})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextoPrincipalColor
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            misSolicitudes.forEach { sol ->
                                                TarjetaMiSolicitudCuidador(
                                                    solicitud = sol,
                                                    onCancelar = {
                                                        viewModel.cancelarSolicitudIntercambio(familyGroupId, sol.id)
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }

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

                            // Sección "Pendientes de tu respuesta" para el Cuidador
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

                            // Banner "Turnos registrados fuera de esta semana"
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
                                                puedeVerFinanzas = com.example.cuidafamily.util.Permisos.puedeVerFinanzas(userRole),
                                                tarifaPorHora = uiState.tarifasMap[itemConEstado.turno.id],
                                                canDelete = esAdmin,
                                                excepciones = uiState.excepcionesTurno,
                                                solicitudes = uiState.solicitudesIntercambio,
                                                onEditClick = { turnoToEdit = itemConEstado.turno },
                                                onDeleteClick = { turnoToDelete = itemConEstado.turno },
                                                onReasignarClick = { turnoToReassign = itemConEstado.turno },
                                                onSolicitarCambio = {
                                                    turnoParaSolicitarCambio = Pair(itemConEstado.turno, itemConEstado.fechaStr)
                                                },
                                                onCancelarSolicitud = { solicitudId ->
                                                    viewModel.cancelarSolicitudIntercambio(familyGroupId, solicitudId)
                                                },
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
        val puedeVerFinanzas = com.example.cuidafamily.util.Permisos.puedeVerFinanzas(userRole)
        val initialTarifa = uiState.tarifasMap[turnoToEdit?.id]

        CrearTurnoDialog(
            miembros = uiState.miembrosGrupo,
            initialTurno = turnoToEdit,
            initialTarifa = initialTarifa,
            puedeVerFinanzas = puedeVerFinanzas,
            isSaving = uiState.isSaving,
            onDismiss = {
                showCrearDialog = false
                turnoToEdit = null
            },
            onCrear = { cuidadorUserId, cuidadorNombre, tel, dias, hIni, hFin, tareas, fInicio, fFin, tarifaPorHora ->
                if (turnoToEdit != null) {
                    val nuevo = turnoToEdit!!.copy(
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
                    viewModel.modificarTurno(familyGroupId, turnoToEdit, nuevo, tarifaPorHora, userRole)
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
                        fechaFin = fFin,
                        tarifaPorHora = tarifaPorHora,
                        userRole = userRole
                    )
                    showCrearDialog = false
                }
            }
        )
    }

    if (showConfigDomicilioDialog) {
        ConfigurarDomicilioDialog(
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

    if (turnoToDelete != null) {
        AlertDialog(
            onDismissRequest = { turnoToDelete = null },
            title = { Text("Cancelar Turno", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("¿Deseas cancelar este turno? El cuidador asignado recibirá un aviso de que el turno fue desactivado.", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = turnoToDelete?.id ?: ""
                        if (id.isNotBlank()) {
                            viewModel.desactivarTurno(familyGroupId, id)
                        }
                        turnoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar Turno", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { turnoToDelete = null }, shape = RoundedCornerShape(12.dp)) {
                    Text("Volver", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (turnoToReassign != null) {
        ReasignarTurnoDialog(
            turno = turnoToReassign!!,
            miembros = uiState.miembrosGrupo,
            isSaving = uiState.isSaving,
            onDismiss = { turnoToReassign = null },
            onReasignar = { nId, nNombre, nTel ->
                viewModel.reasignarTurno(familyGroupId, turnoToReassign!!.id, nId, nNombre, nTel)
                turnoToReassign = null
            }
        )
    }

    if (solicitudToAprobar != null) {
        AprobarSolicitudDialog(
            solicitud = solicitudToAprobar!!,
            miembros = uiState.miembrosGrupo,
            isSaving = uiState.isSaving,
            onDismiss = { solicitudToAprobar = null },
            onAprobar = { rId, rNombre ->
                viewModel.aprobarSolicitudIntercambio(
                    familyGroupId,
                    solicitudToAprobar!!,
                    rId,
                    rNombre,
                    realUserId,
                    userName
                )
                solicitudToAprobar = null
            }
        )
    }

    if (solicitudToRechazar != null) {
        RechazarSolicitudDialog(
            solicitud = solicitudToRechazar!!,
            isSaving = uiState.isSaving,
            onDismiss = { solicitudToRechazar = null },
            onRechazar = { motivo ->
                viewModel.rechazarSolicitudIntercambio(
                    familyGroupId,
                    solicitudToRechazar!!.id,
                    motivo,
                    realUserId,
                    userName
                )
                solicitudToRechazar = null
            }
        )
    }

    if (turnoParaSolicitarCambio != null) {
        val (t, fStr) = turnoParaSolicitarCambio!!
        SolicitarCambioDialog(
            turno = t,
            fechaStr = fStr,
            miembros = uiState.miembrosGrupo,
            currentUserId = realUserId,
            isSaving = uiState.isSaving,
            onDismiss = { turnoParaSolicitarCambio = null },
            onSubmit = { motivo, tipo, rId, rNombre ->
                val sol = SolicitudIntercambio(
                    familyGroupId = familyGroupId,
                    turnoId = t.id,
                    fecha = fStr,
                    horaInicio = t.horaInicio,
                    horaFin = t.horaFin,
                    solicitanteUserId = realUserId,
                    solicitanteNombre = userName,
                    tipo = tipo,
                    reemplazoPropuestoUserId = rId,
                    reemplazoPropuestoNombre = rNombre,
                    motivo = motivo
                )
                viewModel.crearSolicitudIntercambio(familyGroupId, sol, t)
                turnoParaSolicitarCambio = null
            }
        )
    }
}

@Composable
fun TarjetaSolicitudAdmin(
    solicitud: SolicitudIntercambio,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    Surface(
        color = Color(0xFFFFF8E1),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFFFE082)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 ${solicitud.solicitanteNombre}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextoPrincipalColor
                )
                Surface(
                    color = Color(0xFFFFE082),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (solicitud.tipo == TipoIntercambio.PROPONER) "Propone reemplazo" else "Pide asignación",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("📅 Fecha: ${solicitud.fecha} (${formatHora24(solicitud.horaInicio)} - ${formatHora24(solicitud.horaFin)})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LavandaPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Motivo: \"${solicitud.motivo}\"", fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = TextoSecundarioColor)

            if (solicitud.tipo == TipoIntercambio.PROPONER && solicitud.reemplazoPropuestoNombre.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Reemplazo propuesto: ${solicitud.reemplazoPropuestoNombre}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAprobar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("Aprobar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onRechazar,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("Rechazar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
            }
        }
    }
}

@Composable
fun TarjetaReemplazoPendiente(
    excepcion: ExcepcionTurno,
    onAceptar: () -> Unit,
    onRechazar: () -> Unit
) {
    Surface(
        color = Color(0xFFEDE7F6),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "📅 Cubrir turno del ${excepcion.fecha}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = LavandaPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Titular original: ${excepcion.cuidadorOriginalNombre}", fontSize = 13.sp, color = TextoPrincipalColor)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAceptar,
                    colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("Puedo cubrirlo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onRechazar,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("No puedo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
            }
        }
    }
}

@Composable
fun TarjetaMiSolicitudCuidador(
    solicitud: SolicitudIntercambio,
    onCancelar: () -> Unit
) {
    val (estadoBg, estadoText, estadoLabel) = when (solicitud.estado) {
        EstadoSolicitud.PENDIENTE -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "⏳ Pendiente de aprobación")
        EstadoSolicitud.APROBADA -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "✅ Aprobada")
        EstadoSolicitud.RECHAZADA -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "❌ Rechazada")
        EstadoSolicitud.CANCELADA -> Triple(Color(0xFFEEEEEE), Color.Gray, "🚫 Cancelada")
    }

    Surface(
        color = Color(0xFFFAFAFA),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 ${solicitud.fecha} (${formatHora24(solicitud.horaInicio)} - ${formatHora24(solicitud.horaFin)})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextoPrincipalColor
                )

                Surface(color = estadoBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = estadoLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = estadoText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Motivo: \"${solicitud.motivo}\"", fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = TextoSecundarioColor)

            if (solicitud.estado == EstadoSolicitud.RECHAZADA && solicitud.motivoRechazo.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Motivo rechazo: ${solicitud.motivoRechazo}", fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
            }

            if (solicitud.estado == EstadoSolicitud.PENDIENTE) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancelar) {
                        Text("Cancelar solicitud", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }
        }
    }
}

@Composable
fun TurnoCard(
    turnoConEstado: TurnoConEstado,
    currentUserId: String = "",
    userRole: Role = Role.CUIDADOR_EXTERNO,
    puedeVerFinanzas: Boolean = false,
    tarifaPorHora: Double? = null,
    canDelete: Boolean,
    excepciones: List<ExcepcionTurno> = emptyList(),
    solicitudes: List<SolicitudIntercambio> = emptyList(),
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onReasignarClick: () -> Unit = {},
    onSolicitarCambio: () -> Unit = {},
    onCancelarSolicitud: (String) -> Unit = {},
    onCallClick: (String) -> Unit,
    onResponderConfirmacion: (EstadoConfirmacion, String) -> Unit = { _, _ -> }
) {
    val turno = turnoConEstado.turno
    val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val fechaLocalDate = try { LocalDate.parse(turnoConEstado.fechaStr) } catch (e: Exception) { LocalDate.now() }

    val cuidadorResult = cuidadorEnFecha(turno, fechaLocalDate, excepciones, solicitudes)
    val esAdmin = userRole == Role.ADMINISTRADOR_FAMILIAR

    val showButtons = (turno.cuidadorUserId == currentUserId && turno.confirmacion == EstadoConfirmacion.PENDIENTE)

    val (badgeBg, badgeText, badgeLabel) = when (turnoConEstado.estado) {
        EstadoTurno.PROGRAMADO -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Programado")
        EstadoTurno.EN_CURSO -> Triple(Color(0xFFEDE7F6), Color(0xFF673AB7), "En Curso")
        EstadoTurno.COMPLETADO -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Completado")
    }

    var showMotivoDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (cuidadorResult.esReemplazo) Color(0xFF81C784) else LavandaPrimary.copy(alpha = 0.15f)),
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

                // Requerimiento 1: Si el usuario tiene acceso financiero, mostrar la tarifa o aviso "Sin tarifa"
                if (puedeVerFinanzas) {
                    Spacer(modifier = Modifier.width(6.dp))
                    if (tarifaPorHora != null && tarifaPorHora > 0.0) {
                        Surface(
                            color = LavandaPrimaryLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${formatearSoles(tarifaPorHora)} / h",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LavandaPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Text(
                                text = "⚠️ Sin tarifa",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (cuidadorResult.esReemplazo) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF81C784))
                    ) {
                        Text(
                            text = "🔄 Reemplazo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (cuidadorResult.tieneSolicitudPendiente) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D))
                    ) {
                        Text(
                            text = "⏳ Cambio solicitado",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (canDelete) {
                    TextButton(onClick = onEditClick) {
                        Text("Editar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary)
                    }
                    TextButton(onClick = onDeleteClick) {
                        Text("Eliminar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${formatHora24(turno.horaInicio)} - ${formatHora24(turno.horaFin)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(nombre = cuidadorResult.nombre, size = 32.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    if (cuidadorResult.esReemplazo) {
                        Text(
                            text = "🔄 ${cuidadorResult.nombre}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Titular: ${turno.cuidadorNombre}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = cuidadorResult.nombre,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextoPrincipalColor
                        )
                    }
                }

                if (turno.cuidadorTelefono.isNotBlank()) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { onCallClick(turno.cuidadorTelefono) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Llamar", tint = LavandaPrimary)
                    }
                }
            }

            if (turno.tareas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Tareas del turno:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor)
                Spacer(modifier = Modifier.height(4.dp))
                turno.tareas.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = LavandaPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t, fontSize = 13.sp, color = TextoSecundarioColor)
                    }
                }
            }

            if (showButtons) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onResponderConfirmacion(EstadoConfirmacion.CONFIRMADO, "") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Confirmar turno", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { showMotivoDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("No disponible", fontSize = 13.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // BOTÓN DE INTERCAMBIO DE TURNO PARA CUIDADOR (48dp de alto)
            val esMiTurno = (cuidadorResult.userId == currentUserId || turno.cuidadorUserId == currentUserId)
            if (!esAdmin && esMiTurno && turnoConEstado.fechaStr >= hoyStr) {
                Spacer(modifier = Modifier.height(12.dp))
                if (cuidadorResult.tieneSolicitudPendiente && cuidadorResult.solicitudPendiente?.solicitanteUserId == currentUserId) {
                    OutlinedButton(
                        onClick = {
                            cuidadorResult.solicitudPendiente?.let { sol ->
                                onCancelarSolicitud(sol.id)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Cancelar solicitud de cambio", fontSize = 14.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }
                } else if (!cuidadorResult.esReemplazo && !cuidadorResult.tieneSolicitudPendiente) {
                    Button(
                        onClick = onSolicitarCambio,
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Solicitar cambio de este turno", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showMotivoDialog) {
        var motivoText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMotivoDialog = false },
            title = { Text("Indica el motivo", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text("Explica brevemente por qué no puedes cubrir este turno:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = motivoText,
                        onValueChange = { motivoText = it },
                        label = { Text("Motivo (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMotivoDialog = false
                        onResponderConfirmacion(EstadoConfirmacion.RECHAZADO, motivoText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enviar respuesta", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showMotivoDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SolicitarCambioDialog(
    turno: Turno,
    fechaStr: String,
    miembros: List<GroupMemberInfo>,
    currentUserId: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (motivo: String, tipo: TipoIntercambio, reemplazoId: String, reemplazoNombre: String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoIntercambio.CEDER) }
    var reemplazoId by remember { mutableStateOf("") }
    var reemplazoNombre by remember { mutableStateOf("") }
    var errorMotivo by remember { mutableStateOf<String?>(null) }

    val otrosCuidadores = miembros.filter {
        it.member.role == Role.CUIDADOR_EXTERNO && it.member.userId != currentUserId
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar cambio de turno", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Fecha: $fechaStr (${formatHora24(turno.horaInicio)} - ${formatHora24(turno.horaFin)})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LavandaPrimary
                )

                OutlinedTextField(
                    value = motivo,
                    onValueChange = {
                        motivo = it
                        if (it.isNotBlank()) errorMotivo = null
                    },
                    label = { Text("Motivo del cambio (obligatorio)") },
                    isError = errorMotivo != null,
                    supportingText = { errorMotivo?.let { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("¿Cómo deseas realizar el cambio?", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = tipo == TipoIntercambio.CEDER,
                        onClick = { tipo = TipoIntercambio.CEDER }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Que el Administrador asigne a alguien", fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = tipo == TipoIntercambio.PROPONER,
                        onClick = { tipo = TipoIntercambio.PROPONER }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Proponer a un cuidador concreto", fontSize = 13.sp)
                }

                if (tipo == TipoIntercambio.PROPONER) {
                    Text("Selecciona el cuidador propuesto:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (otrosCuidadores.isEmpty()) {
                        Text("No hay otros cuidadores externos registrados.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            otrosCuidadores.forEach { memberInfo ->
                                FilterChip(
                                    selected = reemplazoId == memberInfo.member.userId,
                                    onClick = {
                                        reemplazoId = memberInfo.member.userId
                                        reemplazoNombre = memberInfo.userName
                                    },
                                    label = { Text(memberInfo.userName) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (motivo.isBlank()) {
                        errorMotivo = "El motivo es obligatorio."
                        return@Button
                    }
                    if (tipo == TipoIntercambio.PROPONER && reemplazoId.isBlank()) {
                        errorMotivo = "Selecciona un cuidador propuesto."
                        return@Button
                    }
                    onSubmit(motivo, tipo, reemplazoId, reemplazoNombre)
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enviar solicitud", fontWeight = FontWeight.Bold, color = Color.White)
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
fun AprobarSolicitudDialog(
    solicitud: SolicitudIntercambio,
    miembros: List<GroupMemberInfo>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAprobar: (reemplazoUserId: String, reemplazoNombre: String) -> Unit
) {
    val otrosCuidadores = miembros.filter { it.member.role == Role.CUIDADOR_EXTERNO }
    var selectedUserId by remember {
        mutableStateOf(
            if (solicitud.reemplazoPropuestoUserId.isNotBlank()) solicitud.reemplazoPropuestoUserId
            else otrosCuidadores.firstOrNull()?.member?.userId ?: ""
        )
    }
    var selectedNombre by remember {
        mutableStateOf(
            if (solicitud.reemplazoPropuestoNombre.isNotBlank()) solicitud.reemplazoPropuestoNombre
            else otrosCuidadores.firstOrNull()?.userName ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aprobar cambio de turno", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Solicitante: ${solicitud.solicitanteNombre}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Fecha: ${solicitud.fecha} (${formatHora24(solicitud.horaInicio)} - ${formatHora24(solicitud.horaFin)})", fontSize = 13.sp)
                Text("Motivo: \"${solicitud.motivo}\"", fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                if (solicitud.tipo == TipoIntercambio.PROPONER && solicitud.reemplazoPropuestoNombre.isNotBlank()) {
                    Text("Propuesto por el solicitante: ${solicitud.reemplazoPropuestoNombre}", fontSize = 13.sp, color = LavandaPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Asignar reemplazo a:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                if (otrosCuidadores.isEmpty()) {
                    Text("No hay otros cuidadores externos disponibles.", fontSize = 12.sp, color = Color.Red)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        otrosCuidadores.forEach { memberInfo ->
                            FilterChip(
                                selected = selectedUserId == memberInfo.member.userId,
                                onClick = {
                                    selectedUserId = memberInfo.member.userId
                                    selectedNombre = memberInfo.userName
                                },
                                label = { Text(memberInfo.userName) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUserId.isNotBlank()) {
                        onAprobar(selectedUserId, selectedNombre)
                    }
                },
                enabled = !isSaving && selectedUserId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Aprobar y asignar", fontWeight = FontWeight.Bold, color = Color.White)
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
fun RechazarSolicitudDialog(
    solicitud: SolicitudIntercambio,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onRechazar: (motivoRechazo: String) -> Unit
) {
    var motivoRechazo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rechazar solicitud de cambio", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Solicitante: ${solicitud.solicitanteNombre}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Fecha: ${solicitud.fecha} (${formatHora24(solicitud.horaInicio)} - ${formatHora24(solicitud.horaFin)})", fontSize = 13.sp)

                OutlinedTextField(
                    value = motivoRechazo,
                    onValueChange = { motivoRechazo = it },
                    label = { Text("Motivo de rechazo (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onRechazar(motivoRechazo) },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Rechazar solicitud", fontWeight = FontWeight.Bold, color = Color.White)
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
fun TarjetaRespuestaRecienteAdmin(
    turno: Turno,
    onMarcarVisto: () -> Unit,
    onReasignar: () -> Unit
) {
    val esRechazado = turno.confirmacion == EstadoConfirmacion.RECHAZADO
    val colorBadge = if (esRechazado) Color(0xFFC62828) else Color(0xFF2E7D32)

    Surface(
        color = Color(0xFFFAFAFA),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 ${turno.cuidadorNombre}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextoPrincipalColor
                )

                Surface(
                    color = colorBadge.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (esRechazado) "❌ Rechazado" else "✅ Confirmado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorBadge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Horario: ${formatHora24(turno.horaInicio)} - ${formatHora24(turno.horaFin)}",
                fontSize = 13.sp,
                color = TextoSecundarioColor
            )

            if (esRechazado && turno.motivoRechazo.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Motivo: \"${turno.motivoRechazo}\"", fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color(0xFFC62828))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (esRechazado) {
                    Button(
                        onClick = onReasignar,
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Reasignar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                OutlinedButton(
                    onClick = onMarcarVisto,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Marcar visto", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun TarjetaTurnoPendienteRespuesta(
    turno: Turno,
    onConfirmar: () -> Unit,
    onRechazar: (String) -> Unit,
    onSolicitarReemplazo: (String) -> Unit
) {
    var showRechazarDialog by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFFFFF8E1),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFFFE082)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "⏰ Horario: ${formatHora24(turno.horaInicio)} - ${formatHora24(turno.horaFin)}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            if (turno.tareas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tareas: ${turno.tareas.joinToString(", ")}", fontSize = 13.sp, color = TextoSecundarioColor)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirmar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("Confirmar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = { showRechazarDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Text("No disponible", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
            }
        }
    }

    if (showRechazarDialog) {
        var motivoText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRechazarDialog = false },
            title = { Text("Indica el motivo", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Text("Explica brevemente por qué no puedes cubrir este turno:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = motivoText,
                        onValueChange = { motivoText = it },
                        label = { Text("Motivo (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRechazarDialog = false
                        onRechazar(motivoText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enviar respuesta", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRechazarDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun BannerDestacadoTurno(
    userRole: Role,
    currentUserId: String,
    viewModel: TurnoViewModel,
    familyGroupId: String,
    onRequestLocation: (() -> Unit) -> Unit,
    fusedLocationClient: FusedLocationProviderClient
) {
    val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val diasSemanaHoy = viewModel.getDiasDeLaSemanaActual(0).firstOrNull { it.fechaStr == hoyStr }

    if (diasSemanaHoy == null) return

    val turnosDeHoy = viewModel.obtenerTurnosParaDia(diasSemanaHoy)
    val turnoEnCursoItem = turnosDeHoy.firstOrNull { it.estado == EstadoTurno.EN_CURSO }
    val esCuidador = userRole == Role.CUIDADOR_EXTERNO

    if (turnoEnCursoItem != null && esCuidador) {
        val turno = turnoEnCursoItem.turno
        val checkIn = turnoEnCursoItem.checkIn
        val context = LocalContext.current

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, LavandaPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Surface(
                    color = LavandaPrimaryLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "⚡ Turno en curso ahora",
                        color = LavandaPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${formatHora24(turno.horaInicio)} - ${formatHora24(turno.horaFin)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (checkIn != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (checkIn.ubicacionValida) "✅ Entrada registrada por GPS (${checkIn.horaCheckIn})" else "⚠️ Entrada registrada fuera de rango (${checkIn.horaCheckIn})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (checkIn.ubicacionValida) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                } else if (turno.cuidadorUserId == currentUserId) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onRequestLocation {
                                @SuppressLint("MissingPermission")
                                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                    .addOnSuccessListener { loc ->
                                        if (loc != null) {
                                            viewModel.registrarCheckIn(familyGroupId, turno.id, currentUserId, loc.latitude, loc.longitude)
                                        } else {
                                            Toast.makeText(context, "No se pudo obtener la ubicación GPS actual.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Error obteniendo ubicación GPS.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Registrar entrada por GPS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SemanaSelectorBar(
    offsetSemanas: Int,
    diasSemana: List<DiaSemanaItem>,
    onAnteriorClick: () -> Unit,
    onSiguienteClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val primerDia = diasSemana.firstOrNull()
    val ultimoDia = diasSemana.lastOrNull()
    val textoRango = if (primerDia != null && ultimoDia != null) {
        "${primerDia.numeroDiaMes} - ${ultimoDia.numeroDiaMes} ${formatMesEspanol(primerDia.fechaStr)}"
    } else {
        "Semana actual"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAnteriorClick) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Semana anterior", tint = LavandaPrimary)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onResetClick() }
            ) {
                Text(
                    text = if (offsetSemanas == 0) "Semana Actual" else "Semana (${if (offsetSemanas > 0) "+$offsetSemanas" else offsetSemanas})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LavandaPrimary
                )
                Text(
                    text = textoRango,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextoPrincipalColor
                )
            }

            IconButton(onClick = onSiguienteClick) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Semana siguiente", tint = LavandaPrimary)
            }
        }
    }
}

@Composable
fun CrearTurnoDialog(
    miembros: List<GroupMemberInfo>,
    initialTurno: Turno?,
    initialTarifa: Double? = null,
    puedeVerFinanzas: Boolean = false,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCrear: (
        cuidadorUserId: String,
        cuidadorNombre: String,
        cuidadorTelefono: String,
        diasSemana: List<Int>,
        horaInicio: String,
        horaFin: String,
        tareas: List<String>,
        fechaInicio: Long,
        fechaFin: Long?,
        tarifaPorHora: Double?
    ) -> Unit
) {
    val cuidadores = miembros.filter { it.member.role == Role.CUIDADOR_EXTERNO }

    var selectedUserId by remember { mutableStateOf(initialTurno?.cuidadorUserId ?: (cuidadores.firstOrNull()?.member?.userId ?: "")) }
    var selectedNombre by remember { mutableStateOf(initialTurno?.cuidadorNombre ?: (cuidadores.firstOrNull()?.userName ?: "")) }
    var selectedTelefono by remember { mutableStateOf(initialTurno?.cuidadorTelefono ?: "") }

    var selectedDias by remember { mutableStateOf(initialTurno?.diasSemana ?: listOf(2, 3, 4, 5, 6)) }
    var horaInicio by remember { mutableStateOf(initialTurno?.horaInicio ?: "08:00") }
    var horaFin by remember { mutableStateOf(initialTurno?.horaFin ?: "16:00") }
    var tareasText by remember { mutableStateOf(initialTurno?.tareas?.joinToString("\n") ?: "") }

    var tarifaText by remember {
        mutableStateOf(
            if (initialTarifa != null && initialTarifa > 0.0)
                DecimalFormat("0.00", DecimalFormatSymbols(Locale.US)).format(initialTarifa)
            else ""
        )
    }

    var fechaInicioMs by remember { mutableStateOf(initialTurno?.fechaInicio ?: 0L) }
    var fechaFinMs by remember { mutableStateOf<Long?>(initialTurno?.fechaFin) }

    var showHoraIniPicker by remember { mutableStateOf(false) }
    var showHoraFinPicker by remember { mutableStateOf(false) }
    var showFechaInicioDialog by remember { mutableStateOf(false) }
    var showFechaFinDialog by remember { mutableStateOf(false) }

    val diasLabels = listOf("L", "M", "X", "J", "V", "S", "D")
    val diasValues = listOf(2, 3, 4, 5, 6, 7, 1)

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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (initialTurno == null) "Asignar Nuevo Turno" else "Editar Turno",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Column {
                    Text("Selecciona el Cuidador:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextoPrincipalColor)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (cuidadores.isEmpty()) {
                        Text("No hay cuidadores externos en el grupo familiar.", fontSize = 12.sp, color = Color.Red)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            cuidadores.forEach { memberInfo ->
                                FilterChip(
                                    selected = selectedUserId == memberInfo.member.userId,
                                    onClick = {
                                        selectedUserId = memberInfo.member.userId
                                        selectedNombre = memberInfo.userName
                                    },
                                    label = { Text(memberInfo.userName) }
                                )
                            }
                        }
                    }
                }

                // Campo 'Tarifa por hora (S/)' SOLO para quien tenga puedeVerFinanzas
                if (puedeVerFinanzas) {
                    OutlinedTextField(
                        value = tarifaText,
                        onValueChange = { input ->
                            if (input.isBlank() || input.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                                tarifaText = input
                            }
                        },
                        label = { Text("Tarifa por hora (S/)") },
                        prefix = { Text("S/ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Column {
                    Text("Días de trabajo semanales:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextoPrincipalColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        diasLabels.forEachIndexed { idx, label ->
                            val dayVal = diasValues[idx]
                            val isSel = selectedDias.contains(dayVal)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedDias = if (isSel) selectedDias.filter { it != dayVal } else selectedDias + dayVal
                                },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showHoraIniPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Entrada: ${formatHora24(horaInicio)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showHoraFinPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salida: ${formatHora24(horaFin)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    val fIniText = if (fechaInicioMs > 0L) formatFechaMs(fechaInicioMs) else "Sin inicio"
                    OutlinedButton(
                        onClick = { showFechaInicioDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Inicio: $fIniText", fontSize = 12.sp)
                    }

                    val fFinText = if (fechaFinMs != null && fechaFinMs!! > 0L) formatFechaMs(fechaFinMs!!) else "Sin fin"
                    OutlinedButton(
                        onClick = { showFechaFinDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fin: $fFinText", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = tareasText,
                    onValueChange = { tareasText = it },
                    label = { Text("Tareas o instrucciones del turno (una por línea)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val tareasList = tareasText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                            val tarifaVal = tarifaText.toDoubleOrNull()
                            onCrear(
                                selectedUserId,
                                selectedNombre,
                                selectedTelefono,
                                selectedDias,
                                horaInicio,
                                horaFin,
                                tareasList,
                                fechaInicioMs,
                                fechaFinMs,
                                tarifaVal
                            )
                        },
                        enabled = !isSaving && selectedUserId.isNotBlank() && selectedDias.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar Turno", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showHoraIniPicker) {
        Dialog(onDismissRequest = { showHoraIniPicker = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hora de Entrada", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LavandaPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    var tempTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
                    WheelTimePicker(horaInicial = tempTime) { tempTime = it }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            horaInicio = tempTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                            showHoraIniPicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aceptar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showHoraFinPicker) {
        Dialog(onDismissRequest = { showHoraFinPicker = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hora de Salida", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LavandaPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    var tempTime by remember { mutableStateOf(LocalTime.of(16, 0)) }
                    WheelTimePicker(horaInicial = tempTime) { tempTime = it }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            horaFin = tempTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                            showHoraFinPicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aceptar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showFechaInicioDialog) {
        SelectorFechaDialog(
            fechaInicial = if (fechaInicioMs > 0L) Instant.ofEpochMilli(fechaInicioMs).atZone(ZoneOffset.UTC).toLocalDate() else LocalDate.now(),
            titulo = "Fecha de inicio del turno",
            onFechaSeleccionada = { fecha ->
                fechaInicioMs = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                showFechaInicioDialog = false
            },
            onCancelar = { showFechaInicioDialog = false }
        )
    }

    if (showFechaFinDialog) {
        SelectorFechaDialog(
            fechaInicial = if (fechaFinMs != null && fechaFinMs!! > 0L) Instant.ofEpochMilli(fechaFinMs!!).atZone(ZoneOffset.UTC).toLocalDate() else LocalDate.now(),
            titulo = "Fecha de fin del turno (opcional)",
            onFechaSeleccionada = { fecha ->
                fechaFinMs = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                showFechaFinDialog = false
            },
            onCancelar = { showFechaFinDialog = false }
        )
    }
}

@Composable
fun ConfigurarDomicilioDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onRequestLocation: (() -> Unit) -> Unit,
    fusedLocationClient: FusedLocationProviderClient,
    onGuardar: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var latText by remember { mutableStateOf("") }
    var lngText by remember { mutableStateOf("") }
    var isObtainingGps by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Domicilio", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Establece las coordenadas GPS del domicilio para validar el Check-In de los cuidadores.", fontSize = 13.sp, color = TextoSecundarioColor)

                Button(
                    onClick = {
                        onRequestLocation {
                            isObtainingGps = true
                            @SuppressLint("MissingPermission")
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { loc ->
                                    isObtainingGps = false
                                    if (loc != null) {
                                        latText = loc.latitude.toString()
                                        lngText = loc.longitude.toString()
                                        Toast.makeText(context, "Ubicación GPS capturada.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No se pudo obtener la ubicación GPS.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    isObtainingGps = false
                                    Toast.makeText(context, "Error obteniendo ubicación.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isObtainingGps) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Usar mi ubicación GPS actual", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitud") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitud") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull()
                    val lng = lngText.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        onGuardar(lat, lng)
                    } else {
                        Toast.makeText(context, "Ingresa coordenadas válidas.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar Domicilio", fontWeight = FontWeight.Bold, color = Color.White)
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
fun ReasignarTurnoDialog(
    turno: Turno,
    miembros: List<GroupMemberInfo>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onReasignar: (nuevoUserId: String, nuevoNombre: String, nuevoTelefono: String) -> Unit
) {
    val cuidadores = miembros.filter { it.member.role == Role.CUIDADOR_EXTERNO && it.member.userId != turno.cuidadorUserId }

    var selectedUserId by remember { mutableStateOf(cuidadores.firstOrNull()?.member?.userId ?: "") }
    var selectedNombre by remember { mutableStateOf(cuidadores.firstOrNull()?.userName ?: "") }
    var selectedTel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reasignar Turno", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Horario: ${formatHora24(turno.horaInicio)} - ${formatHora24(turno.horaFin)}", fontSize = 13.sp, color = TextoSecundarioColor)
                Text("Cuidador actual: ${turno.cuidadorNombre}", fontSize = 13.sp, color = TextoPrincipalColor)

                Spacer(modifier = Modifier.height(4.dp))
                Text("Selecciona el nuevo Cuidador:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                if (cuidadores.isEmpty()) {
                    Text("No hay otros cuidadores externos en el grupo.", fontSize = 12.sp, color = Color.Red)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        cuidadores.forEach { memberInfo ->
                            FilterChip(
                                selected = selectedUserId == memberInfo.member.userId,
                                onClick = {
                                    selectedUserId = memberInfo.member.userId
                                    selectedNombre = memberInfo.userName
                                },
                                label = { Text(memberInfo.userName) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUserId.isNotBlank()) {
                        onReasignar(selectedUserId, selectedNombre, selectedTel)
                    }
                },
                enabled = !isSaving && selectedUserId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reasignar", fontWeight = FontWeight.Bold, color = Color.White)
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

fun formatHora24(horaStr: String): String {
    if (horaStr.isBlank()) return ""
    return horaStr.padStart(5, '0')
}

fun formatMesEspanol(fechaStr: String): String {
    if (fechaStr.isBlank()) return ""
    return try {
        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdfIn.parse(fechaStr) ?: Date()
        val sdfOut = SimpleDateFormat("MMM", Locale("es", "ES"))
        sdfOut.format(date).replace(".", "").replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        ""
    }
}

fun formatFechaMs(ms: Long): String {
    if (ms <= 0L) return ""
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ms))
}

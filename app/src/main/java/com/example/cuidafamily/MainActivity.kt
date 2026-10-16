package com.example.cuidafamily

import android.os.Bundle
import android.os.Build
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cuidafamily.model.EstadoConfirmacion
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.repository.CalendarRepository
import com.example.cuidafamily.repository.PatientRepository
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.service.CuidaFamilyMonitorService
import com.example.cuidafamily.ui.auth.AuthFlowContainer
import com.example.cuidafamily.ui.auth.AuthViewModel
import com.example.cuidafamily.ui.auth.SplashScreen
import com.example.cuidafamily.ui.bitacora.BitacoraScreenContainer
import com.example.cuidafamily.ui.bitacora.BitacoraViewModel
import com.example.cuidafamily.ui.calendar.CalendarScreenContainer
import com.example.cuidafamily.ui.calendar.CalendarViewModel
import com.example.cuidafamily.ui.calendar.ConfirmacionAsistenciaScreen
import com.example.cuidafamily.ui.document.DocumentoScreenContainer
import com.example.cuidafamily.ui.document.DocumentoViewModel
import com.example.cuidafamily.ui.export.ExportarHistorialScreen
import com.example.cuidafamily.ui.group.GrupoFamiliarScreen
import com.example.cuidafamily.ui.patient.PatientScreenContainer
import com.example.cuidafamily.ui.patient.PatientViewModel
import com.example.cuidafamily.ui.sos.SosConfirmationDialog
import com.example.cuidafamily.ui.sos.SosFloatingButton
import com.example.cuidafamily.ui.sos.SosIncomingAlertOverlayDialog
import com.example.cuidafamily.ui.sos.SosViewModel
import com.example.cuidafamily.ui.theme.CuidaFamilyTheme
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.turnos.TurnoScreenContainer
import com.example.cuidafamily.ui.turnos.TurnoViewModel
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.GradientButton
import com.example.cuidafamily.ui.util.UserAvatar
import com.example.cuidafamily.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    private val patientViewModel: PatientViewModel by viewModels()
    private val calendarViewModel: CalendarViewModel by viewModels()
    private val sosViewModel: SosViewModel by viewModels()
    private val bitacoraViewModel: BitacoraViewModel by viewModels()
    private val documentoViewModel: DocumentoViewModel by viewModels()
    private val turnoViewModel: TurnoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        NotificationHelper.inicializarCanales(this)

        setContent {
            CuidaFamilyTheme {
                val context = LocalContext.current
                val authState by authViewModel.uiState.collectAsState()
                val sosState by sosViewModel.uiState.collectAsState()

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    Log.d("TurnoNotif", "Permiso POST_NOTIFICATIONS estado: $isGranted")
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val isGranted = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        Log.d("TurnoNotif", "Permiso POST_NOTIFICATIONS estado inicial: $isGranted")
                        if (!isGranted) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                LaunchedEffect(authState.familyGroupId, authState.nombre, authState.authStatus) {
                    val activeFirebaseUser = FirebaseAuth.getInstance().currentUser
                    val groupId = authState.familyGroupId
                    val userId = activeFirebaseUser?.uid ?: ""
                    val userName = authState.nombre
                    if (!groupId.isNullOrBlank() && userId.isNotBlank()) {
                        sosViewModel.iniciarEscucha(groupId, userId, userName, context)

                        val patientResult = PatientRepository.obtenerPatientDelGrupo(groupId)
                        val patientNombre = if (patientResult is Result.Success && patientResult.data != null) {
                            patientResult.data.nombre.ifBlank { "el adulto mayor" }
                        } else {
                            "el adulto mayor"
                        }

                        bitacoraViewModel.cargarBitacora(groupId, userId, context, patientNombre)

                        CuidaFamilyMonitorService.iniciarServicio(context, groupId, userId, userName, patientNombre)
                    } else {
                        bitacoraViewModel.limpiarSesion()
                        CuidaFamilyMonitorService.detenerServicio(context)
                    }
                }

                val navController = rememberNavController()

                val intentDestination = (context as? ComponentActivity)?.intent?.getStringExtra("NAVIGATE_TO")
                LaunchedEffect(intentDestination) {
                    if (intentDestination == "bitacora") {
                        (context as? ComponentActivity)?.intent?.removeExtra("NAVIGATE_TO")
                        navController.navigate("bitacora") {
                            launchSingleTop = true
                        }
                    } else if (intentDestination == "turnos") {
                        (context as? ComponentActivity)?.intent?.removeExtra("NAVIGATE_TO")
                        navController.navigate("turnos") {
                            launchSingleTop = true
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = "splash",
                            enterTransition = {
                                fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { it / 4 })
                            },
                            exitTransition = {
                                slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut(animationSpec = tween(300))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { -it / 4 })
                            },
                            popExitTransition = {
                                slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut(animationSpec = tween(300))
                            }
                        ) {
                            composable("splash") {
                                SplashScreen(
                                    viewModel = authViewModel,
                                    onNavigateNext = {
                                        val destination = if (authState.familyGroupId != null) "home" else "auth_flow"
                                        navController.navigate(destination) {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("auth_flow") {
                                AuthFlowContainer(
                                    viewModel = authViewModel,
                                    onAuthSuccess = {
                                        navController.navigate("home") {
                                            popUpTo("auth_flow") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("home") {
                                val bitacoraState by bitacoraViewModel.uiState.collectAsState()
                                val allEventsMap by CalendarRepository.allEventsFlow.collectAsState()
                                val turnoState by turnoViewModel.uiState.collectAsState()
                                val activeUser = FirebaseAuth.getInstance().currentUser
                                val familyGroupId = authState.familyGroupId ?: ""
                                val userId = activeUser?.uid ?: ""

                                LaunchedEffect(familyGroupId, userId) {
                                    if (familyGroupId.isNotBlank() && userId.isNotBlank()) {
                                        bitacoraViewModel.calcularNovedadesNoVistas(familyGroupId, userId)
                                        turnoViewModel.cargarDatos(familyGroupId)
                                    }
                                }

                                val pendingCalendarCount = allEventsMap.values.count { ev ->
                                    ev.asignadoAUserId == userId && ev.confirmacionAsistencia == EstadoConfirmacion.PENDIENTE
                                }

                                val unreadRespuestasCount = if (authState.selectedRole == Role.ADMINISTRADOR_FAMILIAR) {
                                    val count = turnoState.turnosActivos.count { !it.respuestaVistaPorAdmin && it.confirmacion != EstadoConfirmacion.PENDIENTE }
                                    Log.d("TurnoNotif", "Respuestas sin ver: $count")
                                    count
                                } else {
                                    0
                                }

                                val pendingTurnosCount = turnoState.turnosActivos.count { t ->
                                    t.cuidadorUserId == userId && t.confirmacion == EstadoConfirmacion.PENDIENTE && !t.esVencido()
                                }

                                val totalTurnosBadge = unreadRespuestasCount + pendingTurnosCount

                                WelcomeScreen(
                                    userName = authState.nombre,
                                    roleName = authState.selectedRole?.name ?: "No asignado",
                                    userRole = authState.selectedRole,
                                    unreadBitacoraCount = bitacoraState.noVistasCount,
                                    pendingCalendarCount = pendingCalendarCount,
                                    pendingTurnosCount = totalTurnosBadge,
                                    onNavigateToFicha = { navController.navigate("ficha") },
                                    onNavigateToCalendario = { navController.navigate("calendario") },
                                    onNavigateToBitacora = { navController.navigate("bitacora") },
                                    onNavigateToTurnos = { navController.navigate("turnos") },
                                    onNavigateToGrupo = { navController.navigate("grupo_familiar") },
                                    onSosClick = { sosViewModel.solicitarConfirmacionSos() },
                                    onLogout = {
                                        CuidaFamilyMonitorService.detenerServicio(context)
                                        turnoViewModel.limpiar()
                                        bitacoraViewModel.limpiar()
                                        calendarViewModel.limpiar()
                                        patientViewModel.limpiar()
                                        sosViewModel.limpiar()
                                        documentoViewModel.limpiar()
                                        authViewModel.cerrarSesion()
                                        navController.navigate("auth_flow") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("ficha") {
                                val familyGroupId = authState.familyGroupId
                                val userRole = authState.selectedRole ?: Role.COLABORADOR
                                val userName = authState.nombre

                                if (familyGroupId != null) {
                                    PatientScreenContainer(
                                        viewModel = patientViewModel,
                                        familyGroupId = familyGroupId,
                                        userRole = userRole,
                                        userName = userName,
                                        onBackClick = { navController.popBackStack() },
                                        onNavigateToDocumentos = { navController.navigate("documentos") },
                                        onSosClick = { sosViewModel.solicitarConfirmacionSos() }
                                    )
                                } else {
                                    ErrorGroupScreen { navController.popBackStack() }
                                }
                            }

                            composable("calendario") {
                                val familyGroupId = authState.familyGroupId
                                val userRole = authState.selectedRole ?: Role.ADMINISTRADOR_FAMILIAR
                                val userName = authState.nombre

                                if (familyGroupId != null) {
                                    CalendarScreenContainer(
                                        viewModel = calendarViewModel,
                                        familyGroupId = familyGroupId,
                                        userRole = userRole,
                                        userName = userName,
                                        onBackClick = { navController.popBackStack() },
                                        onNavigateToConfirmacion = { eventId -> navController.navigate("confirmacion_asistencia/$eventId") },
                                        onSosClick = { sosViewModel.solicitarConfirmacionSos() }
                                    )
                                } else {
                                    ErrorGroupScreen { navController.popBackStack() }
                                }
                            }

                            composable("confirmacion_asistencia/{eventId}") { backStackEntry ->
                                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                                val familyGroupId = authState.familyGroupId
                                val activeUser = FirebaseAuth.getInstance().currentUser
                                val currentUserId = activeUser?.uid ?: ""
                                val userName = authState.nombre

                                if (familyGroupId != null && eventId.isNotBlank()) {
                                    ConfirmacionAsistenciaScreen(
                                        familyGroupId = familyGroupId,
                                        eventId = eventId,
                                        currentUserId = currentUserId,
                                        userName = userName,
                                        onBackClick = { navController.popBackStack() },
                                        onSosClick = { sosViewModel.solicitarConfirmacionSos() }
                                    )
                                } else {
                                    ErrorGroupScreen { navController.popBackStack() }
                                }
                            }

                            composable("bitacora") {
                                val familyGroupId = authState.familyGroupId
                                val userRole = authState.selectedRole ?: Role.COLABORADOR
                                val userName = authState.nombre

                                if (familyGroupId != null) {
                                    BitacoraScreenContainer(
                                        viewModel = bitacoraViewModel,
                                        familyGroupId = familyGroupId,
                                        userRole = userRole,
                                        userName = userName,
                                        onBackClick = { navController.popBackStack() },
                                        onSosClick = { sosViewModel.solicitarConfirmacionSos() }
                                    )
                                } else {
                                    ErrorGroupScreen { navController.popBackStack() }
                                }
                            }

                            composable("documentos") {
                                val familyGroupId = authState.familyGroupId
                                val userRole = authState.selectedRole ?: Role.COLABORADOR
                                val userName = authState.nombre

                                var patientId by remember { mutableStateOf("") }
                                LaunchedEffect(familyGroupId) {
                                    if (familyGroupId != null) {
                                        val pResult = PatientRepository.obtenerPatientDelGrupo(familyGroupId)
                                        if (pResult is Result.Success && pResult.data != null) {
                                            patientId = pResult.data.id
                                        }
                                    }
                                }

                                if (familyGroupId != null && patientId.isNotBlank()) {
                                    DocumentoScreenContainer(
                                        viewModel = documentoViewModel,
                                        familyGroupId = familyGroupId,
                                        patientId = patientId,
                                        userRole = userRole,
                                        userName = userName,
                                        onBackClick = { navController.popBackStack() },
                                        onNavigateToExportar = { navController.navigate("exportar_historial") },
                                        onSosClick = { sosViewModel.solicitarConfirmacionSos() }
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = LavandaPrimary)
                                    }
                                }
                            }

                            composable("exportar_historial") {
                                val familyGroupId = authState.familyGroupId
                                val userRole = authState.selectedRole ?: Role.COLABORADOR
                                val userName = authState.nombre

                                if (familyGroupId != null) {
                                    ExportarHistorialScreen(
                                        familyGroupId = familyGroupId,
                                        userRole = userRole,
                                        userName = userName,
                                        onBackClick = { navController.popBackStack() },
                                        onSosClick = { sosViewModel.solicitarConfirmacionSos() }
                                    )
                                } else {
                                    ErrorGroupScreen { navController.popBackStack() }
                                }
                            }

                            composable("turnos") {
                                val familyGroupId = authState.familyGroupId
                                val userRole = authState.selectedRole ?: Role.CUIDADOR_EXTERNO
                                val userName = authState.nombre
                                val activeUser = FirebaseAuth.getInstance().currentUser
                                val currentUserId = activeUser?.uid ?: ""

                                if (familyGroupId != null) {
                                    TurnoScreenContainer(
                                        viewModel = turnoViewModel,
                                        familyGroupId = familyGroupId,
                                        userRole = userRole,
                                        userName = userName,
                                        currentUserId = currentUserId,
                                        onBackClick = { navController.popBackStack() },
                                        onSosClick = { sosViewModel.solicitarConfirmacionSos() }
                                    )
                                } else {
                                    ErrorGroupScreen { navController.popBackStack() }
                                }
                            }

                            composable("grupo_familiar") {
                                val familyGroupId = authState.familyGroupId
                                val userRole = authState.selectedRole ?: Role.COLABORADOR
                                val userName = authState.nombre

                                if (familyGroupId != null) {
                                    GrupoFamiliarScreen(
                                        familyGroupId = familyGroupId,
                                        userRole = userRole,
                                        userName = userName,
                                        onBackClick = { navController.popBackStack() }
                                    )
                                } else {
                                    ErrorGroupScreen { navController.popBackStack() }
                                }
                            }
                        }

                        if (sosState.showConfirmDialog) {
                            val activeUser = FirebaseAuth.getInstance().currentUser
                            val familyGroupId = authState.familyGroupId ?: ""
                            val userId = activeUser?.uid ?: ""
                            val userName = authState.nombre
                            SosConfirmationDialog(
                                onConfirm = {
                                    sosViewModel.dispararAlerta(familyGroupId, userId, userName)
                                },
                                onDismiss = {
                                    sosViewModel.cancelarConfirmacionSos()
                                }
                            )
                        }

                        sosState.incomingAlert?.let { incomingAlert ->
                            val activeUser = FirebaseAuth.getInstance().currentUser
                            val userId = activeUser?.uid ?: ""
                            val userName = authState.nombre
                            SosIncomingAlertOverlayDialog(
                                alert = incomingAlert,
                                onMarkAttended = {
                                    sosViewModel.marcarComoAtendida(incomingAlert, userId, userName)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorGroupScreen(onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Error: Grupo no encontrado", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No se pudo identificar tu grupo familiar. Por favor, reinicia la sesión.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBackClick) { Text("Volver") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffoldWrapper(
    title: String,
    onNavigateToGrupo: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToGrupo) {
                        Icon(Icons.Default.Groups, contentDescription = "Mi Grupo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun WelcomeScreen(
    userName: String,
    roleName: String, 
    userRole: Role? = null,
    unreadBitacoraCount: Int = 0,
    pendingCalendarCount: Int = 0,
    pendingTurnosCount: Int = 0,
    onNavigateToFicha: () -> Unit,
    onNavigateToCalendario: () -> Unit,
    onNavigateToBitacora: () -> Unit,
    onNavigateToTurnos: () -> Unit,
    onNavigateToGrupo: () -> Unit,
    onSosClick: () -> Unit = {},
    onLogout: () -> Unit
) {
    var showNoticeDialog by remember { mutableStateOf(true) }

    if (showNoticeDialog && pendingTurnosCount > 0 && userRole == Role.CUIDADOR_EXTERNO) {
        AlertDialog(
            onDismissRequest = { showNoticeDialog = false },
            title = { Text("Turnos Pendientes", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    "Tienes $pendingTurnosCount turno(s) pendiente(s) de confirmar. Por favor confirma tu disponibilidad en la app.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoticeDialog = false
                        onNavigateToTurnos()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ver y responder", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showNoticeDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Más tarde", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    AppBackgroundDecorated {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                UserAvatar(nombre = userName, size = 80.dp)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Hola, $userName",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Tu rol: $roleName",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientButton(
                        text = "Ficha Médica",
                        onClick = onNavigateToFicha,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        GradientButton(
                            text = "Calendario / Agenda",
                            onClick = onNavigateToCalendario,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (pendingCalendarCount > 0) {
                            Surface(
                                color = Color(0xFFE65100),
                                shape = CircleShape,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .defaultMinSize(minWidth = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (pendingCalendarCount > 99) "99+" else pendingCalendarCount.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        GradientButton(
                            text = "Bitácora de Novedades",
                            onClick = onNavigateToBitacora,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (unreadBitacoraCount > 0) {
                            Surface(
                                color = Color(0xFFD32F2F),
                                shape = CircleShape,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .defaultMinSize(minWidth = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unreadBitacoraCount > 99) "99+" else unreadBitacoraCount.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        GradientButton(
                            text = "Gestión de Turnos",
                            onClick = onNavigateToTurnos,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (pendingTurnosCount > 0) {
                            Surface(
                                color = Color(0xFFD32F2F),
                                shape = CircleShape,
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .defaultMinSize(minWidth = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (pendingTurnosCount > 99) "99+" else pendingTurnosCount.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onNavigateToGrupo,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.2f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(20.dp), tint = LavandaPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mi Grupo Familiar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar sesión segura", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            SosFloatingButton(
                onClick = onSosClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }
    }
}

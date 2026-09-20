package com.example.cuidafamily

import android.os.Bundle
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.ui.auth.AuthFlowContainer
import com.example.cuidafamily.ui.auth.AuthViewModel
import com.example.cuidafamily.ui.auth.SplashScreen
import com.example.cuidafamily.ui.calendar.CalendarScreenContainer
import com.example.cuidafamily.ui.calendar.CalendarViewModel
import com.example.cuidafamily.ui.group.GrupoFamiliarScreen
import com.example.cuidafamily.ui.patient.PatientScreenContainer
import com.example.cuidafamily.ui.patient.PatientViewModel
import com.example.cuidafamily.ui.theme.CuidaFamilyTheme
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.theme.LavandaPrimaryLight
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.GradientButton
import com.example.cuidafamily.ui.util.UserAvatar

class MainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    private val patientViewModel: PatientViewModel by viewModels()
    private val calendarViewModel: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        setContent {
            CuidaFamilyTheme {
                val navController = rememberNavController()

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
                                val authState by authViewModel.uiState.collectAsState()
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
                                val authState by authViewModel.uiState.collectAsState()
                                WelcomeScreen(
                                    userName = authState.nombre,
                                    roleName = authState.selectedRole?.name ?: "No asignado",
                                    onNavigateToFicha = { navController.navigate("ficha") },
                                    onNavigateToCalendario = { navController.navigate("calendario") },
                                    onNavigateToGrupo = { navController.navigate("grupo_familiar") },
                                    onLogout = {
                                        authViewModel.cerrarSesion()
                                        navController.navigate("auth_flow") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("ficha") {
                                val authState by authViewModel.uiState.collectAsState()
                                val familyGroupId = authState.familyGroupId
                                val userRole = authState.selectedRole ?: Role.COLABORADOR
                                val userName = authState.nombre

                                if (familyGroupId != null) {
                                    PatientScreenContainer(
                                        viewModel = patientViewModel,
                                        familyGroupId = familyGroupId,
                                        userRole = userRole,
                                        userName = userName,
                                        onBackClick = { navController.popBackStack() }
                                    )
                                } else {
                                    ErrorGroupScreen { navController.popBackStack() }
                                }
                            }

                            composable("calendario") {
                                val authState by authViewModel.uiState.collectAsState()
                                val familyGroupId = authState.familyGroupId
                                val userRole = authState.selectedRole ?: Role.ADMINISTRADOR_FAMILIAR
                                val userName = authState.nombre

                                if (familyGroupId != null) {
                                    CalendarScreenContainer(
                                        viewModel = calendarViewModel,
                                        familyGroupId = familyGroupId,
                                        userRole = userRole,
                                        userName = userName,
                                        onBackClick = { navController.popBackStack() }
                                    )
                                } else {
                                    ErrorGroupScreen { navController.popBackStack() }
                                }
                            }

                            composable("grupo_familiar") {
                                val authState by authViewModel.uiState.collectAsState()
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
    onNavigateToFicha: () -> Unit,
    onNavigateToCalendario: () -> Unit,
    onNavigateToGrupo: () -> Unit,
    onLogout: () -> Unit
) {
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
                Spacer(modifier = Modifier.height(40.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GradientButton(
                        text = "Ficha Médica",
                        onClick = onNavigateToFicha,
                        modifier = Modifier.fillMaxWidth()
                    )

                    GradientButton(
                        text = "Calendario / Agenda",
                        onClick = onNavigateToCalendario,
                        modifier = Modifier.fillMaxWidth()
                    )

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

                    Spacer(modifier = Modifier.height(20.dp))

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
        }
    }
}

package com.example.cuidafamily.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.ui.theme.AdminGradientEnd
import com.example.cuidafamily.ui.theme.AdminGradientStart
import com.example.cuidafamily.ui.theme.CollabGradientEnd
import com.example.cuidafamily.ui.theme.CollabGradientStart
import com.example.cuidafamily.ui.theme.CuidadorGradientEnd
import com.example.cuidafamily.ui.theme.CuidadorGradientStart
import com.example.cuidafamily.ui.theme.GradientStarEnd
import com.example.cuidafamily.ui.theme.GradientStarStart
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.theme.LavandaPrimaryLight
import com.example.cuidafamily.ui.theme.RoleCuidadorColor
import com.example.cuidafamily.ui.theme.TextoSecundario
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.GradientButton

/**
 * Componente de Logo compartido para el flujo de autenticación.
 */
@Composable
fun AuthLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = GradientStarStart
            )
            .background(
                Brush.linearGradient(listOf(GradientStarStart, GradientStarEnd)),
                RoundedCornerShape(22.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.VolunteerActivism,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }
}

/**
 * Enumeración interna para guiar la navegación del flujo en memoria.
 */
enum class AuthScreenDestination {
    LOGIN,
    REGISTRO,
    SELECCION_ROL,
    SUBTIPO_ROL,
    CONFIGURACION_GRUPO
}

@Composable
fun AuthFlowContainer(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(AuthScreenDestination.LOGIN) }
    val uiState by viewModel.uiState.collectAsState()

    AppBackgroundDecorated {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                AuthScreenDestination.LOGIN -> {
                    LoginScreen(
                        uiState = uiState,
                        onEmailOrPhoneChanged = viewModel::onCorreoOTelefonoChanged,
                        onPasswordChanged = viewModel::onContraseniaChanged,
                        onLoginClick = {
                            viewModel.login()
                        },
                        onNavigateToRegister = { 
                            viewModel.resetAuthStatus()
                            currentScreen = AuthScreenDestination.REGISTRO 
                        }
                    )
                }
                AuthScreenDestination.REGISTRO -> {
                    RegistroScreen(
                        uiState = uiState,
                        onNombreChanged = viewModel::onNombreChanged,
                        onEmailOrPhoneChanged = viewModel::onCorreoOTelefonoChanged,
                        onPasswordChanged = viewModel::onContraseniaChanged,
                        onNextClick = { 
                            viewModel.resetAuthStatus()
                            currentScreen = AuthScreenDestination.SELECCION_ROL 
                        },
                        onNavigateToLogin = { 
                            viewModel.resetAuthStatus()
                            currentScreen = AuthScreenDestination.LOGIN 
                        }
                    )
                }
                AuthScreenDestination.SELECCION_ROL -> {
                    SeleccionRolScreen(
                        uiState = uiState,
                        onRoleSelected = viewModel::onRoleSelected,
                        onBackClick = { currentScreen = AuthScreenDestination.REGISTRO },
                        onNextClick = { currentScreen = AuthScreenDestination.SUBTIPO_ROL }
                    )
                }
                AuthScreenDestination.SUBTIPO_ROL -> {
                    SubtipoRolScreen(
                        uiState = uiState,
                        onSubtipoSelected = viewModel::onSubtipoSelected,
                        onParentescoChanged = viewModel::onParentescoPersonalizadoChanged,
                        onBackClick = { currentScreen = AuthScreenDestination.SELECCION_ROL },
                        onNextClick = { currentScreen = AuthScreenDestination.CONFIGURACION_GRUPO }
                    )
                }
                AuthScreenDestination.CONFIGURACION_GRUPO -> {
                    ConfiguracionGrupoScreen(
                        uiState = uiState,
                        onGroupNameChanged = viewModel::onNombreGrupoChanged,
                        onInviteCodeChanged = viewModel::onCodigoInvitacionChanged,
                        onBackClick = { currentScreen = AuthScreenDestination.SUBTIPO_ROL },
                        onRegisterClick = {
                            viewModel.registrarUsuarioYConfigurarGrupo()
                        }
                    )
                }
            }

            // Manejo de Estados de Carga superpuestos
            when (val status = uiState.authStatus) {
                is AuthStatus.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is AuthStatus.Success -> {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.9f)).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "¡Acceso Correcto!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = status.message,
                                textAlign = TextAlign.Center,
                                color = TextoSecundario
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            GradientButton(
                                text = "Comenzar ahora",
                                onClick = {
                                    viewModel.resetAuthStatus()
                                    onAuthSuccess()
                                }
                            )
                        }
                    }
                }
                is AuthStatus.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(status.message, textAlign = TextAlign.Center, color = TextoSecundario)
                                Spacer(modifier = Modifier.height(24.dp))
                                GradientButton(
                                    text = "Entendido",
                                    onClick = viewModel::resetAuthStatus
                                )
                            }
                        }
                    }
                }
                AuthStatus.Idle -> { /* No hacer nada */ }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onEmailOrPhoneChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AuthLogo()
            
            Spacer(modifier = Modifier.height(20.dp))

            Text("Bienvenido de nuevo", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text("Tu red de cuidado familiar", fontSize = 14.sp, color = TextoSecundario)
            
            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = uiState.correoOTelefono,
                onValueChange = onEmailOrPhoneChanged,
                label = { Text("Correo o Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary, 
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.contrasenia,
                onValueChange = onPasswordChanged,
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary, 
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = "Entrar a la app",
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("¿Aún no tienes cuenta? Regístrate", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    uiState: AuthUiState,
    onNombreChanged: (String) -> Unit,
    onEmailOrPhoneChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNextClick: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AuthLogo()
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Paso 1 de 4", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Datos personales", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = onNombreChanged,
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.correoOTelefono,
                onValueChange = onEmailOrPhoneChanged,
                label = { Text("Correo o Teléfono") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.contrasenia,
                onValueChange = onPasswordChanged,
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            GradientButton(
                text = "Siguiente: Elegir Rol",
                onClick = onNextClick,
                enabled = uiState.nombre.isNotBlank() && uiState.correoOTelefono.isNotBlank() && uiState.contrasenia.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("¿Ya tienes cuenta? Inicia sesión", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionRolScreen(
    uiState: AuthUiState,
    onRoleSelected: (Role) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Elige tu Rol", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("Atrás", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "¿Cuál es tu papel?", 
                fontSize = 26.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Personaliza tu experiencia de cuidado", 
                fontSize = 14.sp, 
                color = TextoSecundario,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                RolTarjetaAnimada(
                    titulo = "Administrador Familiar",
                    ejemplo = "Ej: Hijo mayor, tutor legal.",
                    descripcion = "Control total del grupo familiar y configuración completa.",
                    icono = Icons.Default.SupervisorAccount,
                    decoIcon = Icons.Default.Favorite,
                    startColor = AdminGradientStart,
                    endColor = AdminGradientEnd,
                    isSelected = uiState.selectedRole == Role.ADMINISTRADOR_FAMILIAR,
                    onClick = { onRoleSelected(Role.ADMINISTRADOR_FAMILIAR) }
                )

                RolTarjetaAnimada(
                    titulo = "Colaborador",
                    ejemplo = "Ej: Familiares cercanos.",
                    descripcion = "Acceso compartido a la agenda, visitas y notas.",
                    icono = Icons.Default.Person,
                    decoIcon = Icons.Default.VolunteerActivism,
                    startColor = CollabGradientStart,
                    endColor = CollabGradientEnd,
                    isSelected = uiState.selectedRole == Role.COLABORADOR,
                    onClick = { onRoleSelected(Role.COLABORADOR) }
                )

                RolTarjetaAnimada(
                    titulo = "Cuidador Externo",
                    ejemplo = "Ej: Enfermeros, asistentes.",
                    descripcion = "Registro de signos vitales y reportes de turno.",
                    icono = Icons.Default.AssignmentInd,
                    decoIcon = Icons.Default.MedicalServices,
                    startColor = CuidadorGradientStart,
                    endColor = CuidadorGradientEnd,
                    isSelected = uiState.selectedRole == Role.CUIDADOR_EXTERNO,
                    onClick = { onRoleSelected(Role.CUIDADOR_EXTERNO) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GradientButton(
                text = "Continuar registro",
                onClick = onNextClick,
                enabled = uiState.selectedRole != null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtipoRolScreen(
    uiState: AuthUiState,
    onSubtipoSelected: (String) -> Unit,
    onParentescoChanged: (String) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val role = uiState.selectedRole ?: return
    val (title, subtitle, icon) = when (role) {
        Role.ADMINISTRADOR_FAMILIAR -> Triple("¿Cuál es tu parentesco?", "Indica tu relación con la persona cuidada", Icons.Default.Favorite)
        Role.COLABORADOR -> Triple("¿Cuál es tu parentesco?", "Personaliza tu perfil dentro del equipo", Icons.Default.Handshake)
        Role.CUIDADOR_EXTERNO -> Triple("¿Cuál es tu perfil?", "Ayuda a la familia a identificarte mejor", Icons.Default.MedicalServices)
    }
    
    val options = when (role) {
        Role.ADMINISTRADOR_FAMILIAR -> listOf("Hijo/a", "Hermano/a")
        Role.COLABORADOR -> listOf("Familiar / Pariente", "Enfermero/a", "Cuidador/a Externo")
        Role.CUIDADOR_EXTERNO -> listOf("Enfermero/a", "Cuidador/a")
    }

    val accentColor = when (role) {
        Role.ADMINISTRADOR_FAMILIAR -> AdminGradientStart
        Role.COLABORADOR -> CollabGradientStart
        Role.CUIDADOR_EXTERNO -> CuidadorGradientStart
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Personalizar Perfil", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) { Text("Atrás", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(60.dp).background(accentColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(text = subtitle, fontSize = 14.sp, color = TextoSecundario, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (role == Role.COLABORADOR) {
                    // Vista simplificada para Colaborador: Solo campo de texto libre
                    Text(
                        text = "Escribe tu relación con la persona bajo cuidado para que el resto del equipo te identifique.",
                        fontSize = 14.sp,
                        color = TextoSecundario,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = uiState.parentescoPersonalizado,
                        onValueChange = onParentescoChanged,
                        label = { Text("Parentesco (ej. Nieto/a, Sobrino/a)") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            focusedLabelColor = accentColor
                        ),
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false)
                    )
                } else {
                    // Opciones fijas para Admin y Cuidador Externo
                    options.forEach { option ->
                        OptionCard(
                            title = option,
                            isSelected = uiState.subtipo == option,
                            accentColor = accentColor,
                            onClick = { onSubtipoSelected(option) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            GradientButton(
                text = "Continuar",
                onClick = onNextClick,
                enabled = uiState.isSubtipoContinuarEnabled,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun OptionCard(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.shadow(elevation = if (isSelected) 8.dp else 2.dp, shape = RoundedCornerShape(16.dp), spotColor = if (isSelected) accentColor else Color.Black),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, if (isSelected) accentColor else Color.Transparent),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).border(2.dp, if (isSelected) accentColor else Color.LightGray, CircleShape).padding(4.dp), contentAlignment = Alignment.Center) {
                if (isSelected) Box(modifier = Modifier.fillMaxSize().background(accentColor, CircleShape))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isSelected) accentColor else Color.DarkGray)
        }
    }
}

@Composable
fun RolTarjetaAnimada(
    titulo: String,
    ejemplo: String,
    descripcion: String,
    icono: ImageVector,
    decoIcon: ImageVector,
    startColor: Color,
    endColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.02f else 1f, animationSpec = tween(300), label = "scale")
    val shadowColor = if (isSelected) startColor else Color.LightGray.copy(alpha = 0.3f)

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale).shadow(elevation = if (isSelected) 12.dp else 4.dp, shape = RoundedCornerShape(16.dp), spotColor = shadowColor, ambientColor = shadowColor.copy(alpha = 0.4f)).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(startColor))
            Icon(imageVector = decoIcon, contentDescription = null, tint = startColor.copy(alpha = 0.08f), modifier = Modifier.size(90.dp).align(Alignment.TopEnd).offset(x = 10.dp, y = 10.dp))
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(54.dp).shadow(elevation = 6.dp, shape = CircleShape, spotColor = startColor).background(Brush.linearGradient(listOf(startColor, endColor)), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(text = titulo, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = if (isSelected) startColor else MaterialTheme.colorScheme.onSurface)
                    Text(text = ejemplo, fontSize = 12.sp, color = startColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = descripcion, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 18.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionGrupoScreen(
    uiState: AuthUiState,
    onGroupNameChanged: (String) -> Unit,
    onInviteCodeChanged: (String) -> Unit,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val esAdmin = uiState.selectedRole == Role.ADMINISTRADOR_FAMILIAR
    Scaffold(containerColor = Color.Transparent, topBar = { TopAppBar(title = { Text("Configurar Grupo", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(onClick = onBackClick) { Text("Atrás", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp).verticalScroll(rememberScrollState())) {
            Text("Paso 4 de 4", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (esAdmin) {
                Text("Crea tu Grupo Familiar", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Define el nombre del núcleo familiar. Generaremos un código de 6 dígitos para tus colaboradores.", fontSize = 14.sp, color = TextoSecundario)
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(value = uiState.nombreGrupo, onValueChange = onGroupNameChanged, label = { Text("Nombre de la Familia (ej: Familia García)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(autoCorrectEnabled = false), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White), shape = RoundedCornerShape(16.dp))
            } else {
                Text("Únete a un Grupo", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ingresa el código numérico de 6 dígitos que te proporcionó el Administrador Familiar.", fontSize = 14.sp, color = TextoSecundario)
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(value = uiState.codigoInvitacion, onValueChange = { input -> if (input.all { it.isDigit() } && input.length <= 6) { onInviteCodeChanged(input) } }, label = { Text("Código de invitación (6 dígitos)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, autoCorrectEnabled = false), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White), shape = RoundedCornerShape(16.dp))
            }
            Spacer(modifier = Modifier.height(48.dp))
            GradientButton(text = "Registrarse en CuidaFamily", onClick = onRegisterClick, enabled = uiState.isRegisterEnabled, modifier = Modifier.fillMaxWidth())
        }
    }
}

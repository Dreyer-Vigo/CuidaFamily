package com.example.cuidafamily.ui.patient

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.cuidafamily.model.Allergy
import com.example.cuidafamily.model.Patient
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.Severidad
import com.example.cuidafamily.model.calcularEdad
import com.example.cuidafamily.repository.PatientRepository
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.ui.sos.SosFloatingButton
import com.example.cuidafamily.ui.theme.AmbarAlerta
import com.example.cuidafamily.ui.theme.GradientEnd
import com.example.cuidafamily.ui.theme.GradientStart
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.theme.LavandaPrimaryLight
import com.example.cuidafamily.ui.theme.RojoAlerta
import com.example.cuidafamily.ui.theme.RojoAlertaFondo
import com.example.cuidafamily.ui.theme.TextoPrincipalColor
import com.example.cuidafamily.ui.theme.TextoSecundario
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.NOMBRES_MESES_ESP
import com.example.cuidafamily.ui.util.SelectorFechaDialog
import com.example.cuidafamily.ui.util.UserAvatar
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Requerimiento 3: Muestra la fecha de nacimiento formateada en español.
 * Ejemplo: "18 de marzo de 1948"
 */
fun formatFechaNacimientoEspanol(fechaIso: String): String {
    if (fechaIso.isBlank()) return "Seleccionar fecha de nacimiento"
    return try {
        val date = LocalDate.parse(fechaIso)
        val mesNombre = NOMBRES_MESES_ESP[date.monthValue - 1].lowercase()
        "${date.dayOfMonth} de $mesNombre de ${date.year}"
    } catch (e: Exception) {
        fechaIso
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientScreenContainer(
    viewModel: PatientViewModel,
    familyGroupId: String,
    userRole: Role,
    userName: String,
    onBackClick: () -> Unit,
    onNavigateToDocumentos: () -> Unit = {},
    onNavigateToMedicacion: () -> Unit = {},
    onSosClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEditingFull by remember { mutableStateOf(false) }

    LaunchedEffect(familyGroupId, userRole) {
        viewModel.loadPatient(familyGroupId, userRole)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Ficha Médica del Adulto Mayor", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = LavandaPrimary)
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
                when (val state = uiState) {
                    is PatientUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LavandaPrimary)
                        }
                    }
                    is PatientUiState.Empty -> {
                        PatientEmptyState(
                            canEdit = userRole == Role.ADMINISTRADOR_FAMILIAR || userRole == Role.COLABORADOR,
                            onCreateClick = { isEditingFull = true }
                        )
                    }
                    is PatientUiState.Loaded -> {
                        if (isEditingFull && state.canEdit) {
                            PatientFormScreen(
                                patient = state.patient,
                                familyGroupId = familyGroupId,
                                viewModel = viewModel,
                                onSaveSuccess = { isEditingFull = false },
                                onCancel = { isEditingFull = false }
                            )
                        } else {
                            PatientDetailsScreen(
                                patient = state.patient,
                                canEdit = state.canEdit,
                                onEditClick = { isEditingFull = true },
                                onNavigateToDocumentos = onNavigateToDocumentos,
                                onNavigateToMedicacion = onNavigateToMedicacion,
                                viewModel = viewModel,
                                familyGroupId = familyGroupId,
                                userRole = userRole
                            )
                        }
                    }
                    is PatientUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.mensaje}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientEmptyState(
    canEdit: Boolean,
    onCreateClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(LavandaPrimaryLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalInformation,
                    contentDescription = null,
                    tint = LavandaPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Ficha Médica no registrada",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Aún no se han completado los datos del adulto mayor para este grupo familiar.",
                fontSize = 14.sp,
                color = TextoSecundario,
                textAlign = TextAlign.Center
            )
            
            if (canEdit) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCreateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Completar Ficha Médica", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PatientDetailsScreen(
    patient: Patient,
    canEdit: Boolean,
    onEditClick: () -> Unit,
    onNavigateToDocumentos: () -> Unit = {},
    onNavigateToMedicacion: () -> Unit = {},
    viewModel: PatientViewModel,
    familyGroupId: String,
    userRole: Role
) {
    var alergiasList by remember { mutableStateOf<List<Allergy>>(emptyList()) }
    val esAdmin = userRole == Role.ADMINISTRADOR_FAMILIAR
    var showAllergyDialog by remember { mutableStateOf(false) }

    var showEditPersonal by remember { mutableStateOf(false) }
    var showEditClinical by remember { mutableStateOf(false) }
    var showEditEmergency by remember { mutableStateOf(false) }
    var showEditChronic by remember { mutableStateOf(false) }
    var showEditMeds by remember { mutableStateOf(false) }
    var showEditSangrePeso by remember { mutableStateOf(false) }

    LaunchedEffect(patient, familyGroupId) {
        val result = PatientRepository.obtenerAlergiasDelPatient(familyGroupId, patient.id)
        if (result is Result.Success) {
            alergiasList = result.data
        }
        Log.d("FichaRol", "rol=$userRole, mostrando medicamentos=${patient.medicamentosActuales.isNotEmpty()}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Banner Informativo para Cuidador Externo
        if (userRole == Role.CUIDADOR_EXTERNO) {
            Surface(
                color = LavandaPrimaryLight,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = LavandaPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vista de Cuidador Externo: datos para el cuidado diario",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LavandaPrimary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ficha Paciente", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (canEdit) {
                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Formulario Completo", fontSize = 11.sp)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, LavandaPrimary.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(28.dp))
                .clickable(enabled = esAdmin) { showEditPersonal = true },
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF3E8FF)))).padding(24.dp)) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .shadow(elevation = 12.dp, shape = CircleShape, spotColor = LavandaPrimary)
                            .clip(CircleShape)
                            .background(LavandaPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        if (patient.fotoUrl != null) {
                            AsyncImage(model = patient.fotoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text(patient.nombre.take(1).uppercase(), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(patient.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    val edadTexto = if (patient.fechaNacimiento.isNotBlank()) "Edad: ${patient.calcularEdad()} años" else "Edad no registrada"
                    val subtitleTexto = if (userRole != Role.CUIDADOR_EXTERNO) {
                        "$edadTexto  •  DNI: ${patient.dni.ifBlank { "N/A" }}"
                    } else {
                        edadTexto
                    }
                    Text(subtitleTexto, fontSize = 14.sp, color = TextoSecundario)
                }
                if (esAdmin) Icon(Icons.Default.Edit, null, tint = LavandaPrimary.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
            }
        }

        // 2. Sangre y Peso (Visibles para todos los roles, incluido CUIDADOR_EXTERNO)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RojoAlertaFondo),
                modifier = Modifier.weight(1f).clickable(enabled = esAdmin) { showEditSangrePeso = true },
                shape = RoundedCornerShape(20.dp)
            ) {
                Box {
                    Icon(Icons.Default.WaterDrop, null, tint = RojoAlerta.copy(alpha = 0.08f), modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).offset(10.dp, 10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(RojoAlerta))
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sangre", fontSize = 12.sp, color = RojoAlerta, fontWeight = FontWeight.Bold)
                            Text(patient.grupoSanguineo, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = RojoAlerta)
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = LavandaPrimaryLight),
                modifier = Modifier.weight(1f).clickable(enabled = esAdmin) { showEditSangrePeso = true },
                shape = RoundedCornerShape(20.dp)
            ) {
                Box {
                    Icon(Icons.Default.MonitorWeight, null, tint = LavandaPrimary.copy(alpha = 0.08f), modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).offset(10.dp, 10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(LavandaPrimary))
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Peso", fontSize = 12.sp, color = LavandaPrimary, fontWeight = FontWeight.Bold)
                            Text("${patient.pesoActual} Kg", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = LavandaPrimary)
                        }
                    }
                }
            }
        }

        // Acceso a Documentos Médicos / Recetas Digitalizadas (Visible para todos)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToDocumentos() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(LavandaPrimaryLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderShared,
                        contentDescription = null,
                        tint = LavandaPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (userRole == Role.CUIDADOR_EXTERNO) "Ver Recetas Médicas" else "Documentos Médicos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (userRole == Role.CUIDADOR_EXTERNO) "Consulta recetas, dosis e indicaciones médicas" else "Ver y subir recetas, exámenes, radiografías",
                        fontSize = 12.sp,
                        color = TextoSecundario
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = LavandaPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Secciones restringidas (Ocultas para CUIDADOR_EXTERNO)
        if (userRole != Role.CUIDADOR_EXTERNO) {
            PatientInfoSectionCard(
                title = "Datos Clínicos",
                icon = Icons.Default.MedicalInformation,
                color = LavandaPrimary,
                clickable = esAdmin,
                onClick = { showEditClinical = true }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Historia Clínica: ${patient.historiaClinica.ifBlank { "Sin registrar" }}", fontSize = 14.sp)
                    Text("Seguro de Salud: ${patient.seguroSalud.ifBlank { "Sin registrar" }}", fontSize = 14.sp)
                    Text("Médico Tratante: ${patient.medicoTratante.ifBlank { "Sin registrar" }}", fontSize = 14.sp)
                }
            }

            PatientInfoSectionCard(
                title = "Contacto de Emergencia",
                icon = Icons.Default.ContactEmergency,
                color = RojoAlerta,
                clickable = esAdmin,
                onClick = { showEditEmergency = true }
            ) {
                if (patient.contactoEmergenciaNombre.isBlank()) {
                    Text("Sin contacto registrado", color = TextoSecundario, fontSize = 13.sp)
                } else {
                    Column {
                        Text(patient.contactoEmergenciaNombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tel: ${patient.contactoEmergenciaTelefono} (${patient.contactoEmergenciaRelacion})", fontSize = 14.sp)
                    }
                }
            }

            PatientInfoSectionCard(
                title = "Enfermedades Crónicas",
                icon = Icons.Default.HistoryEdu,
                color = AmbarAlerta,
                clickable = esAdmin,
                onClick = { showEditChronic = true }
            ) {
                Text(patient.enfermedadesCronicas.ifBlank { "Ninguna registrada." }, fontSize = 14.sp, lineHeight = 20.sp)
            }

            PatientInfoSectionCard(
                title = "Medicamentos Actuales",
                icon = Icons.Default.Medication,
                color = Color(0xFF4A90E2),
                clickable = esAdmin,
                onClick = { showEditMeds = true }
            ) {
                Column {
                    Text(patient.medicamentosActuales.ifBlank { "Sin medicación reportada." }, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToMedicacion,
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver medicación", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Sección de Alergias Conocidas (Visibles para todos los roles)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, null, tint = LavandaPrimary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Alergias Conocidas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        }
        
        if (alergiasList.isEmpty()) {
            Text("Sin alergias registradas", color = TextoSecundario, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
        } else {
            var allergyToEdit by remember { mutableStateOf<Allergy?>(null) }
            alergiasList.forEach { AlergiaTarjetaDescriptiva(allergy = it, onClick = { if (esAdmin) allergyToEdit = it }) }

            if (allergyToEdit != null) {
                AlergiaDialog(
                    allergy = allergyToEdit,
                    onDismiss = { allergyToEdit = null },
                    onConfirm = { edited ->
                        viewModel.agregarAlergia(edited)
                        allergyToEdit = null
                    }
                )
            }
        }

        if (esAdmin) {
            TextButton(onClick = { showAllergyDialog = true }) {
                Text("+ Agregar Alergia", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        // Medicamentos Actuales para Cuidador Externo
        if (userRole == Role.CUIDADOR_EXTERNO) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF4A90E2).copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEBF3FA), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = Color(0xFF4A90E2),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Medicamentos Actuales",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFFF4F8FC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD6E4F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = patient.medicamentosActuales.ifBlank { "Aún no se registraron medicamentos" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextoPrincipalColor,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onNavigateToDocumentos,
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.FolderShared, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver recetas médicas", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onNavigateToMedicacion,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver control de medicación", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (userRole != Role.CUIDADOR_EXTERNO && patient.informacionAdicional != null) {
            Text("Notas Adicionales", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(patient.informacionAdicional, modifier = Modifier.padding(20.dp), fontSize = 14.sp, color = Color.DarkGray, lineHeight = 22.sp)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    // DIÁLOGOS DE EDICIÓN
    if (showEditPersonal) {
        EditarDatosPersonalesDialog(
            patient = patient,
            onDismiss = { showEditPersonal = false },
            onSave = { cambios ->
                viewModel.actualizarCampos(cambios)
                showEditPersonal = false
            }
        )
    }

    if (showEditSangrePeso) {
        EditarSangrePesoDialog(
            patient = patient,
            onDismiss = { showEditSangrePeso = false },
            onSave = { grupoSanguineo, pesoKg ->
                viewModel.actualizarCampos(mapOf("grupoSanguineo" to grupoSanguineo))
                if (pesoKg != null && pesoKg > 0) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    viewModel.actualizarPeso(sdf.format(Date()), pesoKg)
                }
                showEditSangrePeso = false
            }
        )
    }

    if (showEditClinical) {
        EditarDatosClinicosDialog(
            patient = patient,
            onDismiss = { showEditClinical = false },
            onSave = { cambios ->
                viewModel.actualizarCampos(cambios)
                showEditClinical = false
            }
        )
    }

    if (showEditEmergency) {
        EditarContactoEmergenciaDialog(
            patient = patient,
            onDismiss = { showEditEmergency = false },
            onSave = { cambios ->
                viewModel.actualizarCampos(cambios)
                showEditEmergency = false
            }
        )
    }

    if (showEditChronic) {
        EditarEnfermedadesCronicasDialog(
            patient = patient,
            onDismiss = { showEditChronic = false },
            onSave = { cambios ->
                viewModel.actualizarCampos(cambios)
                showEditChronic = false
            }
        )
    }

    if (showEditMeds) {
        EditarMedicamentosActualesDialog(
            patient = patient,
            onDismiss = { showEditMeds = false },
            onSave = { cambios ->
                viewModel.actualizarCampos(cambios)
                showEditMeds = false
            }
        )
    }

    if (showAllergyDialog) {
        AlergiaDialog(
            allergy = null,
            onDismiss = { showAllergyDialog = false },
            onConfirm = { newAllergy ->
                viewModel.agregarAlergia(newAllergy)
                showAllergyDialog = false
            }
        )
    }
}

/**
 * Requerimiento 1 & 3: Diálogo para editar Datos Personales (Nombre, Fecha de Nacimiento y DNI).
 * Usa un solo contenedor Column con verticalScroll(), imePadding() y SelectorFechaDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarDatosPersonalesDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var nombre by remember { mutableStateOf(patient.nombre) }
    var fechaIso by remember { mutableStateOf(patient.fechaNacimiento) }
    var dni by remember { mutableStateOf(patient.dni) }
    var showDatePicker by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.70f).dp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header del Diálogo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = LavandaPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Editar Datos Personales",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Actualiza los datos de ${patient.nombre}",
                            fontSize = 13.sp,
                            color = TextoSecundario
                        )
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                // Contenido Scrolleable con restricción de altura e IME Padding
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre completo", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = formatFechaNacimientoEspanol(fechaIso),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Fecha de nacimiento", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = LavandaPrimary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color(0xFF1F1B2E),
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = Color(0xFF4B4560),
                            disabledTrailingIconColor = LavandaPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = dni,
                        onValueChange = { input ->
                            if (input.length <= 8 && input.all { it.isDigit() }) {
                                dni = input
                            }
                        },
                        label = { Text("DNI (8 dígitos)", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Botones Fijos de Acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", fontSize = 14.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (nombre.isNotBlank()) {
                                onSave(mapOf("nombre" to nombre.trim(), "fechaNacimiento" to fechaIso, "dni" to dni.trim()))
                            }
                        },
                        enabled = nombre.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Guardar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        SelectorFechaDialog(
            fechaInicial = try { LocalDate.parse(fechaIso) } catch (e: Exception) { LocalDate.now() },
            titulo = "Fecha de Nacimiento",
            fechaMinima = null,
            onFechaSeleccionada = { fecha ->
                fechaIso = fecha.toString()
                showDatePicker = false
            },
            onCancelar = { showDatePicker = false }
        )
    }
}

/**
 * Requerimiento 4: Diálogo para editar Sangre y Peso Actual.
 */
@Composable
fun EditarSangrePesoDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (grupoSanguineo: String, pesoKg: Double?) -> Unit
) {
    var grupoSanguineo by remember { mutableStateOf(patient.grupoSanguineo.ifBlank { "O+" }) }
    var pesoStr by remember { mutableStateOf(if (patient.pesoActual > 0) DecimalFormat("0.0", DecimalFormatSymbols(Locale.US)).format(patient.pesoActual) else "") }

    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.70f).dp
    val opcionesSangre = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = RojoAlerta, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Editar Sangre y Peso", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Actualiza los datos de ${patient.nombre}", fontSize = 13.sp, color = TextoSecundario)
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("Grupo Sanguíneo:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF4B4560))
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(opcionesSangre) { op ->
                                FilterChip(
                                    selected = grupoSanguineo == op,
                                    onClick = { grupoSanguineo = op },
                                    label = { Text(op, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = pesoStr,
                        onValueChange = { input ->
                            if (input.isBlank() || input.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                                pesoStr = input
                            }
                        },
                        label = { Text("Peso Actual", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        singleLine = true,
                        suffix = { Text("kg", fontWeight = FontWeight.Bold, color = LavandaPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", fontSize = 14.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val pVal = pesoStr.toDoubleOrNull()
                            onSave(grupoSanguineo, pVal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Guardar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Requerimiento 4: Diálogo para editar Datos Clínicos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarDatosClinicosDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var historiaClinica by remember { mutableStateOf(patient.historiaClinica) }
    var seguroSalud by remember { mutableStateOf(patient.seguroSalud) }
    var medicoTratante by remember { mutableStateOf(patient.medicoTratante) }

    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.70f).dp
    val opcionesSeguro = listOf("EsSalud", "SIS", "Particular", "Ninguno", "Otro")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = LavandaPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Editar Datos Clínicos", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Actualiza los datos de ${patient.nombre}", fontSize = 13.sp, color = TextoSecundario)
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = historiaClinica,
                        onValueChange = { historiaClinica = it },
                        label = { Text("Historia Clínica / N° expediente", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Column {
                        Text("Seguro de Salud:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF4B4560))
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(opcionesSeguro) { op ->
                                FilterChip(
                                    selected = seguroSalud == op,
                                    onClick = { seguroSalud = op },
                                    label = { Text(op, fontSize = 13.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = seguroSalud,
                        onValueChange = { seguroSalud = it },
                        label = { Text("Nombre o detalle del seguro de salud", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = medicoTratante,
                        onValueChange = { medicoTratante = it },
                        label = { Text("Médico Tratante Principal", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", fontSize = 14.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onSave(mapOf("historiaClinica" to historiaClinica.trim(), "seguroSalud" to seguroSalud.trim(), "medicoTratante" to medicoTratante.trim()))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Guardar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Requerimiento 4: Diálogo para editar Contacto de Emergencia.
 */
@Composable
fun EditarContactoEmergenciaDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var cn by remember { mutableStateOf(patient.contactoEmergenciaNombre) }
    var ct by remember { mutableStateOf(patient.contactoEmergenciaTelefono) }
    var cr by remember { mutableStateOf(patient.contactoEmergenciaRelacion) }

    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.70f).dp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContactEmergency, contentDescription = null, tint = RojoAlerta, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Contacto de Emergencia", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Actualiza los datos de ${patient.nombre}", fontSize = 13.sp, color = TextoSecundario)
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = cn,
                        onValueChange = { cn = it },
                        label = { Text("Nombre del Contacto", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = ct,
                        onValueChange = { ct = it },
                        label = { Text("Teléfono de Emergencia", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = cr,
                        onValueChange = { cr = it },
                        label = { Text("Relación / Parentesco", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", fontSize = 14.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onSave(mapOf("contactoEmergenciaNombre" to cn.trim(), "contactoEmergenciaTelefono" to ct.trim(), "contactoEmergenciaRelacion" to cr.trim()))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Guardar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Requerimiento 4: Diálogo para editar Enfermedades Crónicas.
 */
@Composable
fun EditarEnfermedadesCronicasDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var ec by remember { mutableStateOf(patient.enfermedadesCronicas) }
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.70f).dp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = AmbarAlerta, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Enfermedades Crónicas", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Actualiza los datos de ${patient.nombre}", fontSize = 13.sp, color = TextoSecundario)
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = ec,
                        onValueChange = { ec = it },
                        label = { Text("Enfermedades Crónicas / Diagnósticos", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", fontSize = 14.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(mapOf("enfermedadesCronicas" to ec.trim())) },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Guardar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Requerimiento 4: Diálogo para editar Medicamentos Actuales.
 */
@Composable
fun EditarMedicamentosActualesDialog(
    patient: Patient,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var ma by remember { mutableStateOf(patient.medicamentosActuales) }
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.70f).dp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = Color(0xFF4A90E2), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Medicamentos Actuales", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Actualiza los datos de ${patient.nombre}", fontSize = 13.sp, color = TextoSecundario)
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = ma,
                        onValueChange = { ma = it },
                        label = { Text("Medicamentos Actuales (Dosis e Indicaciones)", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", fontSize = 14.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(mapOf("medicamentosActuales" to ma.trim())) },
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Guardar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Requerimiento 4: Diálogo para agregar o editar una Alergia.
 */
@Composable
fun AlergiaDialog(
    allergy: Allergy?,
    onDismiss: () -> Unit,
    onConfirm: (Allergy) -> Unit
) {
    var nombreAlergeno by remember { mutableStateOf(allergy?.nombreAlergeno ?: "") }
    var severidad by remember { mutableStateOf(allergy?.severidad ?: Severidad.LEVE) }
    var descripcionReaccion by remember { mutableStateOf(allergy?.descripcionReaccion ?: "") }

    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.70f).dp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = LavandaPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (allergy == null) "Agregar Alergia" else "Editar Alergia",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Registra o actualiza alergia conocida",
                            fontSize = 13.sp,
                            color = TextoSecundario
                        )
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = nombreAlergeno,
                        onValueChange = { nombreAlergeno = it },
                        label = { Text("Alergeno (ej: Penicilina, Avispas, Polen)", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Column {
                        Text("Severidad:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF4B4560))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Severidad.values().forEach { sev ->
                                FilterChip(
                                    selected = severidad == sev,
                                    onClick = { severidad = sev },
                                    label = { Text(sev.name, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = descripcionReaccion,
                        onValueChange = { descripcionReaccion = it },
                        label = { Text("Descripción de la reacción (opcional)", fontSize = 14.sp, color = Color(0xFF4B4560)) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", fontSize = 14.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (nombreAlergeno.isNotBlank()) {
                                val newAllergy = Allergy(
                                    id = allergy?.id ?: UUID.randomUUID().toString(),
                                    nombreAlergeno = nombreAlergeno.trim(),
                                    severidad = severidad,
                                    descripcionReaccion = descripcionReaccion.trim()
                                )
                                onConfirm(newAllergy)
                            }
                        },
                        enabled = nombreAlergeno.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Guardar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PatientInfoSectionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    clickable: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = clickable, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                if (clickable) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Edit, null, tint = color.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun AlergiaTarjetaDescriptiva(
    allergy: Allergy,
    onClick: () -> Unit = {}
) {
    val (badgeBg, badgeText, badgeLabel) = when (allergy.severidad) {
        Severidad.LEVE -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Leve")
        Severidad.MODERADA -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Moderada")
        Severidad.SEVERA -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Severa")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(badgeBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = badgeText,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = allergy.nombreAlergeno,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (allergy.descripcionReaccion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reacción: \"${allergy.descripcionReaccion}\"",
                        fontSize = 13.sp,
                        color = TextoSecundario
                    )
                }
            }

            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badgeLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientFormScreen(
    patient: Patient?,
    familyGroupId: String,
    viewModel: PatientViewModel,
    onSaveSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var nombre by remember { mutableStateOf(patient?.nombre ?: "") }
    var fechaNacimiento by remember { mutableStateOf(patient?.fechaNacimiento ?: "") }
    var dni by remember { mutableStateOf(patient?.dni ?: "") }
    var fotoUrl by remember { mutableStateOf(patient?.fotoUrl) }

    var grupoSanguineo by remember { mutableStateOf(patient?.grupoSanguineo ?: "O+") }
    var pesoStr by remember { mutableStateOf(patient?.pesoActual?.toString() ?: "") }
    var historiaClinica by remember { mutableStateOf(patient?.historiaClinica ?: "") }
    var seguroSalud by remember { mutableStateOf(patient?.seguroSalud ?: "") }
    var medicoTratante by remember { mutableStateOf(patient?.medicoTratante ?: "") }

    var contactoEmergenciaNombre by remember { mutableStateOf(patient?.contactoEmergenciaNombre ?: "") }
    var contactoEmergenciaTelefono by remember { mutableStateOf(patient?.contactoEmergenciaTelefono ?: "") }
    var contactoEmergenciaRelacion by remember { mutableStateOf(patient?.contactoEmergenciaRelacion ?: "") }

    var enfermedadesCronicas by remember { mutableStateOf(patient?.enfermedadesCronicas ?: "") }
    var medicamentosActuales by remember { mutableStateOf(patient?.medicamentosActuales ?: "") }
    var infoAdicional by remember { mutableStateOf(patient?.informacionAdicional ?: "") }

    var showAllergyDialog by remember { mutableStateOf(false) }
    var alergiasList by remember { mutableStateOf<List<Allergy>>(emptyList()) }

    LaunchedEffect(patient) {
        if (patient != null) {
            val groupId = patient.familyGroupId
            val result = PatientRepository.obtenerAlergiasDelPatient(groupId, patient.id)
            if (result is Result.Success) {
                alergiasList = result.data
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) { e.printStackTrace() }
                fotoUrl = it.toString()
            }
        }
    )

    var bloodDropdownExpanded by remember { mutableStateOf(false) }
    var seguroDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val opcionesSangre = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    val opcionesSeguro = listOf("Ninguno", "EsSalud", "SIS", "Particular", "Otro")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = if (patient == null) "Alta de Paciente" else "Editar Ficha Completa",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(LavandaPrimaryLight)
                .clickable {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (fotoUrl != null) {
                AsyncImage(
                    model = fotoUrl,
                    contentDescription = "Foto Paciente",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = LavandaPrimary)
                    Text("Subir foto", fontSize = 10.sp, color = LavandaPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("1. Datos Personales Básicos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre Completo") },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formatFechaNacimientoEspanol(fechaNacimiento),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Fecha de Nacimiento") },
                trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = LavandaPrimary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color(0xFF1F1B2E),
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = Color(0xFF4B4560),
                    disabledTrailingIconColor = LavandaPrimary
                )
            )

            OutlinedTextField(
                value = dni,
                onValueChange = { input -> if (input.length <= 8 && input.all { it.isDigit() }) dni = input },
                label = { Text("DNI (8 dígitos)") },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF1F1B2E)),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("2. Datos Médicos Principales", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)

            ExposedDropdownMenuBox(
                expanded = bloodDropdownExpanded,
                onExpandedChange = { bloodDropdownExpanded = !bloodDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = grupoSanguineo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Grupo Sanguíneo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = bloodDropdownExpanded,
                    onDismissRequest = { bloodDropdownExpanded = false }
                ) {
                    opcionesSangre.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op) },
                            onClick = { grupoSanguineo = op; bloodDropdownExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = pesoStr,
                onValueChange = { pesoStr = it },
                label = { Text("Peso Actual (Kg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = historiaClinica,
                onValueChange = { historiaClinica = it },
                label = { Text("N° Historia Clínica") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = seguroDropdownExpanded,
                onExpandedChange = { seguroDropdownExpanded = !seguroDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = seguroSalud,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Seguro de Salud") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seguroDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = seguroDropdownExpanded,
                    onDismissRequest = { seguroDropdownExpanded = false }
                ) {
                    opcionesSeguro.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op) },
                            onClick = { seguroSalud = op; seguroDropdownExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = medicoTratante,
                onValueChange = { medicoTratante = it },
                label = { Text("Médico Tratante Principal") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("3. Contacto de Emergencia Directo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            OutlinedTextField(
                value = contactoEmergenciaNombre,
                onValueChange = { contactoEmergenciaNombre = it },
                label = { Text("Nombre del Contacto") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contactoEmergenciaTelefono,
                onValueChange = { contactoEmergenciaTelefono = it },
                label = { Text("Teléfono de Emergencia") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            OutlinedTextField(
                value = contactoEmergenciaRelacion,
                onValueChange = { contactoEmergenciaRelacion = it },
                label = { Text("Relación / Parentesco") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("4. Cuadro Clínico y Medicación", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            OutlinedTextField(
                value = enfermedadesCronicas,
                onValueChange = { enfermedadesCronicas = it },
                label = { Text("Enfermedades Crónicas / Diagnósticos") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            OutlinedTextField(
                value = medicamentosActuales,
                onValueChange = { medicamentosActuales = it },
                label = { Text("Medicamentos Actuales (Dosis e Indicaciones)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("5. Alergias Conocidas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            alergiasList.forEach { AlergiaTarjetaDescriptiva(allergy = it) }
            OutlinedButton(onClick = { showAllergyDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Alergia")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("6. Notas Adicionales", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            OutlinedTextField(
                value = infoAdicional,
                onValueChange = { infoAdicional = it },
                label = { Text("Cuidados especiales, preferencias, etc.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    if (nombre.isNotBlank()) {
                        val pesoVal = pesoStr.toDoubleOrNull() ?: 0.0
                        viewModel.registrarOEditarPatient(
                            nombre = nombre,
                            fechaNacimiento = fechaNacimiento,
                            dni = dni,
                            grupoSanguineo = grupoSanguineo,
                            pesoActual = pesoVal,
                            historiaClinica = historiaClinica,
                            seguroSalud = seguroSalud,
                            medicoTratante = medicoTratante,
                            contactoEmergenciaNombre = contactoEmergenciaNombre,
                            contactoEmergenciaTelefono = contactoEmergenciaTelefono,
                            contactoEmergenciaRelacion = contactoEmergenciaRelacion,
                            enfermedadesCronicas = enfermedadesCronicas,
                            medicamentosActuales = medicamentosActuales,
                            alergiasIniciales = alergiasList,
                            informacionAdicional = infoAdicional.ifBlank { null },
                            fotoUrl = fotoUrl
                        )
                        onSaveSuccess()
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary)
            ) {
                Text("Guardar Ficha", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showDatePicker) {
        SelectorFechaDialog(
            fechaInicial = try { LocalDate.parse(fechaNacimiento) } catch (e: Exception) { LocalDate.now() },
            titulo = "Fecha de Nacimiento",
            fechaMinima = null,
            onFechaSeleccionada = { fecha ->
                fechaNacimiento = fecha.toString()
                showDatePicker = false
            },
            onCancelar = { showDatePicker = false }
        )
    }

    if (showAllergyDialog) {
        AlergiaDialog(
            allergy = null,
            onDismiss = { showAllergyDialog = false },
            onConfirm = { newAllergy ->
                alergiasList = alergiasList + newAllergy
                showAllergyDialog = false
            }
        )
    }
}

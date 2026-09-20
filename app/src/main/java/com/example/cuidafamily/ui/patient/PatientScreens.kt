package com.example.cuidafamily.ui.patient

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cuidafamily.model.Allergy
import com.example.cuidafamily.model.Patient
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.Severidad
import com.example.cuidafamily.model.calcularEdad
import com.example.cuidafamily.repository.PatientRepository
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.ui.theme.AmbarAlerta
import com.example.cuidafamily.ui.theme.AmbarAlertaFondo
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.theme.LavandaPrimaryLight
import com.example.cuidafamily.ui.theme.RojoAlerta
import com.example.cuidafamily.ui.theme.RojoAlertaFondo
import com.example.cuidafamily.ui.theme.TextoSecundario
import com.example.cuidafamily.ui.util.GradientButton
import com.example.cuidafamily.ui.util.UserAvatar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientScreenContainer(
    viewModel: PatientViewModel,
    familyGroupId: String,
    userRole: Role,
    userName: String,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(familyGroupId, userRole) {
        viewModel.loadPatient(familyGroupId, userRole)
    }

    val diagBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFF3E8FF), Color(0xFFE8FFF3))
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = LavandaPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ficha", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(diagBrush)
        ) {
            Crossfade(targetState = uiState, label = "patientState") { state ->
                when (state) {
                    is PatientUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is PatientUiState.Empty -> {
                        if (isEditing) {
                            PatientFormScreen(
                                patient = null,
                                onSaveClick = { n, f, d, g, p, hc, ss, mt, e_n, e_t, e_r, cr, m, a, i, pic ->
                                    viewModel.registrarOEditarPatient(n, f, d, g, p, hc, ss, mt, e_n, e_t, e_r, cr, m, a, i, pic)
                                    isEditing = false
                                },
                                onCancelClick = { isEditing = false },
                                viewModel = viewModel
                            )
                        } else {
                            PatientEmptyState(
                                userRole = userRole,
                                onRegisterClick = { isEditing = true }
                            )
                        }
                    }
                    is PatientUiState.Loaded -> {
                        if (isEditing && state.canEdit) {
                            PatientFormScreen(
                                patient = state.patient,
                                onSaveClick = { n, f, d, g, p, hc, ss, mt, e_n, e_t, e_r, cr, m, a, i, pic ->
                                    viewModel.registrarOEditarPatient(n, f, d, g, p, hc, ss, mt, e_n, e_t, e_r, cr, m, a, i, pic)
                                    isEditing = false
                                },
                                onCancelClick = { isEditing = false },
                                viewModel = viewModel
                            )
                        } else {
                            PatientDetailsScreen(
                                patient = state.patient,
                                canEdit = state.canEdit,
                                onEditClick = { isEditing = true },
                                viewModel = viewModel,
                                familyGroupId = familyGroupId,
                                userRole = userRole
                            )
                        }
                    }
                    is PatientUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(state.mensaje, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadPatient(familyGroupId, userRole) }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientEmptyState(userRole: Role, onRegisterClick: () -> Unit) {
    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Aún no registraste a la persona bajo cuidado",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (userRole == Role.ADMINISTRADOR_FAMILIAR) {
                Text(
                    text = "Como Administrador, debes dar de alta la ficha médica inicial.",
                    fontSize = 14.sp,
                    color = TextoSecundario,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                GradientButton(
                    text = "Registrar adulto mayor",
                    onClick = onRegisterClick
                )
            } else {
                Text(
                    text = "El administrador de la familia aún no registra los datos clínicos.",
                    fontSize = 14.sp,
                    color = TextoSecundario,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientFormScreen(
    patient: Patient?,
    onSaveClick: (String, String, String, String, Double, String, String, String, String, String, String, String, String, List<Allergy>, String?, String?) -> Unit,
    onCancelClick: () -> Unit,
    viewModel: PatientViewModel
) {
    val context = LocalContext.current
    
    // 1. Datos Personales
    var nombre by remember { mutableStateOf(patient?.nombre ?: "") }
    var fechaNacimiento by remember { mutableStateOf(patient?.fechaNacimiento ?: "") }
    var dni by remember { mutableStateOf(patient?.dni ?: "") }
    var fotoUrl by remember { mutableStateOf(patient?.fotoUrl) }

    // 2. Datos Clínicos
    var grupoSanguineo by remember { mutableStateOf(patient?.grupoSanguineo ?: "O+") }
    var pesoStr by remember { mutableStateOf(patient?.pesoActual?.toString() ?: "") }
    var historiaClinica by remember { mutableStateOf(patient?.historiaClinica ?: "") }
    var seguroSalud by remember { mutableStateOf(patient?.seguroSalud ?: "") }
    var medicoTratante by remember { mutableStateOf(patient?.medicoTratante ?: "") }

    // 3. Contacto de Emergencia
    var contactoEmergenciaNombre by remember { mutableStateOf(patient?.contactoEmergenciaNombre ?: "") }
    var contactoEmergenciaTelefono by remember { mutableStateOf(patient?.contactoEmergenciaTelefono ?: "") }
    var contactoEmergenciaRelacion by remember { mutableStateOf(patient?.contactoEmergenciaRelacion ?: "") }

    // 4. Secciones de Texto
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
    val opcionesSeguro = listOf("Ninguno", "Obra Social", "Prepaga", "Plan Público", "Seguro Privado")

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fechaNacimiento)?.time
        } catch (e: Exception) { System.currentTimeMillis() }
    )

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

        // --- SECCIÓN 1: DATOS PERSONALES ---
        PatientFormSection(title = "Datos Personales") {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
                    .shadow(elevation = 8.dp, shape = CircleShape, spotColor = LavandaPrimary)
                    .clip(CircleShape)
                    .background(LavandaPrimaryLight)
                    .border(BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.3f)), CircleShape)
                    .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (fotoUrl != null) {
                    AsyncImage(model = fotoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = LavandaPrimary, modifier = Modifier.size(40.dp))
                }
                Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.BottomEnd) {
                    Box(modifier = Modifier.size(24.dp).background(LavandaPrimary, CircleShape), contentAlignment = Alignment.Center) {
                        Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                OutlinedTextField(
                    value = fechaNacimiento,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha Nacimiento (AAAA-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = MaterialTheme.colorScheme.primary,
                        disabledTextColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = dni,
                onValueChange = { dni = it },
                label = { Text("DNI / Identificación") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // --- SECCIÓN 2: DATOS CLÍNICOS ---
        PatientFormSection(title = "Datos Clínicos") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = bloodDropdownExpanded,
                    onExpandedChange = { bloodDropdownExpanded = !bloodDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = grupoSanguineo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sangre") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = bloodDropdownExpanded, onDismissRequest = { bloodDropdownExpanded = false }) {
                        opcionesSangre.forEach { op ->
                            DropdownMenuItem(text = { Text(op) }, onClick = { grupoSanguineo = op; bloodDropdownExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = pesoStr,
                    onValueChange = { pesoStr = it },
                    label = { Text("Peso (Kg)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = historiaClinica,
                onValueChange = { historiaClinica = it },
                label = { Text("Nº Historia Clínica") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenuBox(
                expanded = seguroDropdownExpanded,
                onExpandedChange = { seguroDropdownExpanded = !seguroDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = seguroSalud,
                    onValueChange = { seguroSalud = it },
                    label = { Text("Seguro de Salud / Obra Social") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seguroDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = seguroDropdownExpanded, onDismissRequest = { seguroDropdownExpanded = false }) {
                    opcionesSeguro.forEach { op ->
                        DropdownMenuItem(text = { Text(op) }, onClick = { seguroSalud = op; seguroDropdownExpanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = medicoTratante,
                onValueChange = { medicoTratante = it },
                label = { Text("Médico Tratante Principal") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // --- SECCIÓN 3: CONTACTO DE EMERGENCIA ---
        PatientFormSection(title = "Contacto de Emergencia") {
            OutlinedTextField(value = contactoEmergenciaNombre, onValueChange = { contactoEmergenciaNombre = it }, label = { Text("Nombre del Contacto") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = contactoEmergenciaTelefono, onValueChange = { contactoEmergenciaTelefono = it }, label = { Text("Teléfono") }, modifier = Modifier.weight(1.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = contactoEmergenciaRelacion, onValueChange = { contactoEmergenciaRelacion = it }, label = { Text("Relación") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
            }
        }

        // --- SECCIÓN 4: ANTECEDENTES Y MEDICACIÓN ---
        PatientFormSection(title = "Antecedentes y Tratamiento") {
            OutlinedTextField(value = enfermedadesCronicas, onValueChange = { enfermedadesCronicas = it }, label = { Text("Enfermedades Crónicas") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = medicamentosActuales, onValueChange = { medicamentosActuales = it }, label = { Text("Medicamentos Actuales") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = infoAdicional, onValueChange = { infoAdicional = it }, label = { Text("Notas Adicionales (Opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp))
        }

        // --- SECCIÓN 5: ALERGIAS ---
        Text("Alergias e Intolerancias", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
        if (alergiasList.isEmpty()) {
            Text("No se han registrado alergias. Puede añadirlas debajo.", fontSize = 13.sp, color = TextoSecundario)
        } else {
            var allergyToEdit by remember { mutableStateOf<Allergy?>(null) }
            alergiasList.forEach { AlergiaTarjetaDescriptiva(allergy = it, onClick = { allergyToEdit = it }) }
            
            if (allergyToEdit != null) {
                AlergiaDialog(
                    allergy = allergyToEdit,
                    onDismiss = { allergyToEdit = null },
                    onConfirm = { edited ->
                        Log.d("PatientForm", "Editando alergia: ${edited.nombreAlergeno}")
                        if (patient != null) {
                            viewModel.agregarAlergia(edited)
                        } else {
                            alergiasList = alergiasList.map { if (it.id == edited.id) edited else it }
                        }
                        allergyToEdit = null
                    }
                )
            }
        }
        
        TextButton(onClick = { showAllergyDialog = true }, modifier = Modifier.align(Alignment.Start)) {
            Text("+ Agregar Alergia", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCancelClick, modifier = Modifier.weight(1f)) { Text("Cancelar", color = TextoSecundario) }
            GradientButton(
                text = "Guardar ficha",
                onClick = {
                    val peso = pesoStr.toDoubleOrNull() ?: 0.0
                    onSaveClick(nombre, fechaNacimiento, dni, grupoSanguineo, peso, historiaClinica, seguroSalud, medicoTratante, contactoEmergenciaNombre, contactoEmergenciaTelefono, contactoEmergenciaRelacion, enfermedadesCronicas, medicamentosActuales, alergiasList, infoAdicional.ifBlank { null }, fotoUrl)
                },
                enabled = nombre.isNotBlank() && fechaNacimiento.isNotBlank() && pesoStr.isNotBlank(),
                modifier = Modifier.weight(1.5f)
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = ms }
                        fechaNacimiento = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(utcCalendar.time)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAllergyDialog) {
        AlergiaDialog(
            allergy = null,
            onDismiss = { showAllergyDialog = false },
            onConfirm = { newAllergy ->
                Log.d("PatientForm", "Agregando nueva alergia: ${newAllergy.nombreAlergeno}")
                if (patient != null) {
                    viewModel.agregarAlergia(newAllergy)
                } else {
                    alergiasList = alergiasList + newAllergy
                }
                showAllergyDialog = false
            }
        )
    }
}

@Composable
fun PatientFormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun PatientDetailsScreen(
    patient: Patient,
    canEdit: Boolean,
    onEditClick: () -> Unit,
    viewModel: PatientViewModel,
    familyGroupId: String,
    userRole: Role
) {
    var alergiasList by remember { mutableStateOf<List<Allergy>>(emptyList()) }
    val esAdmin = userRole == Role.ADMINISTRADOR_FAMILIAR
    var showAllergyDialog by remember { mutableStateOf(false) }

    // Estados para Diálogos de Edición Rápida
    var showEditPersonal by remember { mutableStateOf(false) }
    var showEditClinical by remember { mutableStateOf(false) }
    var showEditEmergency by remember { mutableStateOf(false) }
    var showEditChronic by remember { mutableStateOf(false) }
    var showEditMeds by remember { mutableStateOf(false) }
    var showEditBlood by remember { mutableStateOf(false) }
    var showEditWeight by remember { mutableStateOf(false) }
    
    LaunchedEffect(patient, familyGroupId) {
        val result = PatientRepository.obtenerAlergiasDelPatient(familyGroupId, patient.id)
        if (result is Result.Success) {
            alergiasList = result.data
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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

        // 1. Tarjeta Personal (Nombre, Foto, Edad, DNI)
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
                    Text("Edad: ${patient.calcularEdad()} años  •  DNI: ${patient.dni.ifBlank { "N/A" }}", fontSize = 14.sp, color = TextoSecundario)
                }
                if (esAdmin) Icon(Icons.Default.Edit, null, tint = LavandaPrimary.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.TopEnd).size(18.dp))
            }
        }

        // 2. Sangre y Peso
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RojoAlertaFondo),
                modifier = Modifier.weight(1f).clickable(enabled = esAdmin) { showEditBlood = true },
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
                modifier = Modifier.weight(1f).clickable(enabled = esAdmin) { showEditWeight = true },
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

        // 3. Datos Clínicos
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

        // 4. Contacto de Emergencia
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

        // 5. Enfermedades Crónicas
        PatientInfoSectionCard(
            title = "Enfermedades Crónicas",
            icon = Icons.Default.HistoryEdu,
            color = AmbarAlerta,
            clickable = esAdmin,
            onClick = { showEditChronic = true }
        ) {
            Text(patient.enfermedadesCronicas.ifBlank { "Ninguna registrada." }, fontSize = 14.sp, lineHeight = 20.sp)
        }

        // 6. Medicamentos Actuales
        PatientInfoSectionCard(
            title = "Medicamentos Actuales",
            icon = Icons.Default.Medication,
            color = Color(0xFF4A90E2),
            clickable = esAdmin,
            onClick = { showEditMeds = true }
        ) {
            Text(patient.medicamentosActuales.ifBlank { "Sin medicación reportada." }, fontSize = 14.sp, lineHeight = 20.sp)
        }

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

        if (patient.informacionAdicional != null) {
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

    // --- DIÁLOGOS DE EDICIÓN RÁPIDA ---
    if (showEditPersonal) {
        QuickEditDialog(
            title = "Editar Datos Personales",
            onDismiss = { showEditPersonal = false },
            onSave = { cambios -> viewModel.actualizarCampos(cambios); showEditPersonal = false }
        ) {
            var n by remember { mutableStateOf(patient.nombre) }
            var f by remember { mutableStateOf(patient.fechaNacimiento) }
            var d by remember { mutableStateOf(patient.dni) }
            OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = f, onValueChange = { f = it }, label = { Text("Fecha Nacimiento (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = d, onValueChange = { d = it }, label = { Text("DNI") }, modifier = Modifier.fillMaxWidth())
            mapOf("nombre" to n, "fechaNacimiento" to f, "dni" to d)
        }
    }

    if (showEditClinical) {
        QuickEditDialog(
            title = "Editar Datos Clínicos",
            onDismiss = { showEditClinical = false },
            onSave = { cambios -> viewModel.actualizarCampos(cambios); showEditClinical = false }
        ) {
            var hc by remember { mutableStateOf(patient.historiaClinica) }
            var ss by remember { mutableStateOf(patient.seguroSalud) }
            var mt by remember { mutableStateOf(patient.medicoTratante) }
            OutlinedTextField(value = hc, onValueChange = { hc = it }, label = { Text("Historia Clínica") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = ss, onValueChange = { ss = it }, label = { Text("Seguro de Salud") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = mt, onValueChange = { mt = it }, label = { Text("Médico Tratante") }, modifier = Modifier.fillMaxWidth())
            mapOf("historiaClinica" to hc, "seguroSalud" to ss, "medicoTratante" to mt)
        }
    }

    if (showEditEmergency) {
        QuickEditDialog(
            title = "Contacto de Emergencia",
            onDismiss = { showEditEmergency = false },
            onSave = { cambios -> viewModel.actualizarCampos(cambios); showEditEmergency = false }
        ) {
            var en by remember { mutableStateOf(patient.contactoEmergenciaNombre) }
            var et by remember { mutableStateOf(patient.contactoEmergenciaTelefono) }
            var er by remember { mutableStateOf(patient.contactoEmergenciaRelacion) }
            OutlinedTextField(value = en, onValueChange = { en = it }, label = { Text("Nombre de Contacto") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = et, onValueChange = { et = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            OutlinedTextField(value = er, onValueChange = { er = it }, label = { Text("Relación (ej: Hijo)") }, modifier = Modifier.fillMaxWidth())
            mapOf("contactoEmergenciaNombre" to en, "contactoEmergenciaTelefono" to et, "contactoEmergenciaRelacion" to er)
        }
    }

    if (showEditChronic) {
        QuickEditDialog(title = "Enfermedades Crónicas", onDismiss = { showEditChronic = false }, onSave = { c -> viewModel.actualizarCampos(c); showEditChronic = false }) {
            var text by remember { mutableStateOf(patient.enfermedadesCronicas) }
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
            mapOf("enfermedadesCronicas" to text)
        }
    }

    if (showEditMeds) {
        QuickEditDialog(title = "Medicamentos Actuales", onDismiss = { showEditMeds = false }, onSave = { c -> viewModel.actualizarCampos(c); showEditMeds = false }) {
            var text by remember { mutableStateOf(patient.medicamentosActuales) }
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Lista de Medicamentos") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
            mapOf("medicamentosActuales" to text)
        }
    }

    if (showEditBlood) {
        QuickEditDialog(title = "Grupo Sanguíneo", onDismiss = { showEditBlood = false }, onSave = { c -> viewModel.actualizarCampos(c); showEditBlood = false }) {
            var g by remember { mutableStateOf(patient.grupoSanguineo) }
            val opciones = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                opciones.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { op ->
                            Surface(
                                onClick = { g = op },
                                color = if (g == op) RojoAlerta else Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, RojoAlerta.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text(op, color = if (g == op) Color.White else RojoAlerta, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
            mapOf("grupoSanguineo" to g)
        }
    }

    if (showEditWeight) {
        QuickEditDialog(title = "Nuevo Registro de Peso", onDismiss = { showEditWeight = false }, onSave = { _ -> /* handled inside */ showEditWeight = false }) {
            var w by remember { mutableStateOf(patient.pesoActual.toString()) }
            Column {
                OutlinedTextField(value = w, onValueChange = { w = it }, label = { Text("Peso en Kg") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(16.dp))
                GradientButton(text = "Guardar Peso", onClick = { 
                    val valKg = w.toDoubleOrNull() ?: 0.0
                    viewModel.actualizarPeso(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()), valKg)
                    showEditWeight = false
                })
            }
            emptyMap<String, Any>() // no direct fields to update here as it uses a function
        }
    }

    if (showAllergyDialog) {
        AlergiaDialog(
            allergy = null,
            onDismiss = { showAllergyDialog = false },
            onConfirm = { newAllergy ->
                Log.d("PatientDetails", "Agregando alergia a Firestore: ${newAllergy.nombreAlergeno}")
                viewModel.agregarAlergia(newAllergy)
                showAllergyDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlergiaDialog(
    allergy: Allergy?,
    onDismiss: () -> Unit,
    onConfirm: (Allergy) -> Unit
) {
    var alergeno by remember { mutableStateOf(allergy?.nombreAlergeno ?: "") }
    var severidad by remember { mutableStateOf(allergy?.severidad ?: Severidad.LEVE) }
    var reaccion by remember { mutableStateOf(allergy?.descripcionReaccion ?: "") }
    var sevExpanded by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (allergy == null) "Nueva Alergia" else "Editar Alergia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = alergeno,
                    onValueChange = { alergeno = it },
                    label = { Text("Alérgeno (ej: Penicilina)") },
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(expanded = sevExpanded, onExpandedChange = { sevExpanded = !sevExpanded }) {
                    OutlinedTextField(
                        value = severidad.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Severidad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sevExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = sevExpanded, onDismissRequest = { sevExpanded = false }) {
                        Severidad.entries.forEach { sev ->
                            DropdownMenuItem(text = { Text(sev.name) }, onClick = {
                                severidad = sev
                                sevExpanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(
                    value = reaccion,
                    onValueChange = { reaccion = it },
                    label = { Text("Reacción Sintomática") },
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cerrar", color = TextoSecundario) }
                    Spacer(modifier = Modifier.width(8.dp))
                    GradientButton(
                        text = if (allergy == null) "Añadir" else "Guardar",
                        onClick = {
                            onConfirm(
                                Allergy(
                                    id = allergy?.id ?: UUID.randomUUID().toString(),
                                    nombreAlergeno = alergeno,
                                    severidad = severidad,
                                    descripcionReaccion = reaccion
                                )
                            )
                        },
                        enabled = alergeno.isNotBlank(),
                        modifier = Modifier.height(44.dp)
                    )
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
    clickable: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
            .clickable(enabled = clickable) { onClick() }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
                if (clickable) Icon(Icons.Default.Edit, null, tint = color.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun QuickEditDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit,
    content: @Composable () -> Map<String, Any>
) {
    var camposParaGuardar by remember { mutableStateOf(emptyMap<String, Any>()) }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                camposParaGuardar = content()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancelar", color = TextoSecundario) }
                    Spacer(modifier = Modifier.width(8.dp))
                    GradientButton(text = "Guardar", onClick = { onSave(camposParaGuardar) }, modifier = Modifier.height(44.dp))
                }
            }
        }
    }
}

/**
 * Tarjeta de alergia detallada con badge de severidad y colores semafóricos.
 */
@Composable
fun AlergiaTarjetaDescriptiva(
    allergy: Allergy,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, textColor, strokeColor) = when (allergy.severidad) {
        Severidad.SEVERA -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), Color(0xFFD32F2F))
        Severidad.MODERADA -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFFE65100))
        Severidad.LEVE -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), Color(0xFFF57F17))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, strokeColor.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Borde izquierdo de severidad
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(strokeColor)
            )

            Box(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = allergy.nombreAlergeno,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        }

                        // Badge de Severidad
                        Surface(
                            color = bgColor,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (allergy.severidad == Severidad.SEVERA) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = allergy.severidad.name,
                                    color = textColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (allergy.descripcionReaccion.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = allergy.descripcionReaccion,
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

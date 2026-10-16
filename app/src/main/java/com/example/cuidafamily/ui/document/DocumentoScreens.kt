package com.example.cuidafamily.ui.document

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.cuidafamily.model.DocumentoMedico
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.model.TipoDocumento
import com.example.cuidafamily.ui.sos.SosFloatingButton
import com.example.cuidafamily.ui.theme.GradientEnd
import com.example.cuidafamily.ui.theme.GradientStart
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.theme.LavandaPrimaryLight
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.UserAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: migrar a Firebase Storage para acceso compartido real entre dispositivos.

import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share

/**
 * Pantalla principal de Documentos Médicos Digitalizados del Paciente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentoScreenContainer(
    viewModel: DocumentoViewModel,
    familyGroupId: String,
    patientId: String,
    userRole: Role,
    userName: String,
    onBackClick: () -> Unit,
    onNavigateToExportar: () -> Unit = {},
    onSosClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showSubirDialog by remember { mutableStateOf(false) }
    var docToDelete by remember { mutableStateOf<DocumentoMedico?>(null) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }

    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(familyGroupId, patientId) {
        viewModel.cargarDocumentos(familyGroupId, patientId)
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

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderShared, contentDescription = null, tint = LavandaPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Documentos Médicos", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                FloatingActionButton(
                    onClick = { showSubirDialog = true },
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .size(56.dp)
                        .background(fabGradient, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Subir Documento", tint = Color.White)
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
                // Opción de Exportar Historial (Solo para Admin y Colaborador)
                if (userRole == Role.ADMINISTRADOR_FAMILIAR || userRole == Role.COLABORADOR) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clickable { onNavigateToExportar() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF3E8FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = LavandaPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Exportar Historial y Bitácora",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Generar informe en PDF o Excel/CSV",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = LavandaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Fila de Chips de Filtro por Tipo de Documento
                FiltroChipsRow(
                    filtroActual = uiState.filtroTipo,
                    onFiltroSelected = { viewModel.setFiltroTipo(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LavandaPrimary)
                        }
                    } else if (uiState.documentosFiltrados.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderShared,
                                        contentDescription = null,
                                        tint = LavandaPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Sin documentos registrados",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sube recetas, exámenes de laboratorio o radiografías para mantenerlos organizados.",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { showSubirDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Subir Primer Documento", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(4.dp)) }

                            items(uiState.documentosFiltrados, key = { it.id }) { doc ->
                                val canDelete = doc.subidoPorUserId == currentUserId || userRole == Role.ADMINISTRADOR_FAMILIAR

                                DocumentoCard(
                                    documento = doc,
                                    canDelete = canDelete,
                                    onDeleteClick = { docToDelete = doc },
                                    onClick = {
                                        if (doc.mimeType.contains("image") || doc.uriLocal.endsWith(".jpg") || doc.uriLocal.endsWith(".png") || doc.uriLocal.endsWith(".jpeg")) {
                                            previewImageUri = doc.uriLocal
                                        } else {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(Uri.parse(doc.uriLocal), doc.mimeType.ifBlank { "*/*" })
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "No hay aplicación compatible para abrir este archivo.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    // Diálogo para Subir Documento Médico
    if (showSubirDialog) {
        SubirDocumentoDialog(
            isSubiendo = uiState.isSubiendo,
            onDismiss = { showSubirDialog = false },
            onSubir = { tipo, nombre, fechaDoc, uriLocal, mimeType ->
                viewModel.subirDocumento(
                    familyGroupId = familyGroupId,
                    patientId = patientId,
                    tipo = tipo,
                    nombre = nombre,
                    fechaDocumento = fechaDoc,
                    uriLocal = uriLocal,
                    mimeType = mimeType,
                    subidoPorUserId = currentUserId,
                    subidoPorNombre = userName
                )
                showSubirDialog = false
            }
        )
    }

    // Diálogo de Confirmación para Eliminar Documento
    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = { Text("Eliminar documento", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas eliminar '${doc.nombre}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarDocumento(familyGroupId, patientId, doc.id)
                        docToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { docToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Vista Previa de Imagen a Pantalla Completa
    previewImageUri?.let { uriStr ->
        Dialog(onDismissRequest = { previewImageUri = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color.Black
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    AsyncImage(
                        model = uriStr,
                        contentDescription = "Documento Médico Ampliado",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                    IconButton(
                        onClick = { previewImageUri = null },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Fila de chips horizontales para filtrar documentos por tipo con mayor visibilidad y legibilidad.
 */
@Composable
fun FiltroChipsRow(
    filtroActual: TipoDocumento?,
    onFiltroSelected: (TipoDocumento?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            val isSelected = filtroActual == null
            Surface(
                onClick = { onFiltroSelected(null) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) LavandaPrimary else Color.White,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) LavandaPrimary else Color.LightGray.copy(alpha = 0.6f)
                ),
                shadowElevation = if (isSelected) 4.dp else 1.dp
            ) {
                Text(
                    text = "Todos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color(0xFF333333),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }

        items(TipoDocumento.entries.toTypedArray()) { tipo ->
            val isSelected = filtroActual == tipo
            Surface(
                onClick = { onFiltroSelected(tipo) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) tipo.colorBackground else Color.White,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) tipo.colorText else Color.LightGray.copy(alpha = 0.6f)
                ),
                shadowElevation = if (isSelected) 4.dp else 1.dp
            ) {
                Text(
                    text = tipo.etiqueta,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) tipo.colorText else Color(0xFF333333),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/**
 * Tarjeta individual para cada documento médico.
 */
@Composable
fun DocumentoCard(
    documento: DocumentoMedico,
    canDelete: Boolean,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    val (icon, iconColor) = when (documento.tipo) {
        TipoDocumento.RECETA -> Pair(Icons.Default.Medication, Color(0xFF2E7D32))
        TipoDocumento.EXAMEN_LABORATORIO -> Pair(Icons.Default.Biotech, Color(0xFF1565C0))
        TipoDocumento.RADIOGRAFIA -> Pair(Icons.Default.FindInPage, Color(0xFF673AB7))
        TipoDocumento.OTRO -> Pair(Icons.Default.InsertDriveFile, Color(0xFFE65100))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.15f)),
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
                    .size(48.dp)
                    .background(documento.tipo.colorBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = documento.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = documento.tipo.colorBackground,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = documento.tipo.etiqueta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = documento.tipo.colorText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Fecha doc: ${documento.fechaDocumento} • Subido por: ${documento.subidoPorNombre}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (canDelete) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Diálogo para formulario de registro y selección de archivo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubirDocumentoDialog(
    isSubiendo: Boolean,
    onDismiss: () -> Unit,
    onSubir: (TipoDocumento, String, String, String, String) -> Unit
) {
    val context = LocalContext.current

    var selectedTipo by remember { mutableStateOf(TipoDocumento.RECETA) }
    var nombreText by remember { mutableStateOf("") }
    var fechaDocText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-DD", Locale.getDefault()).format(Date()))
    }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMimeType by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            selectedFileUri = uri
            selectedMimeType = context.contentResolver.getType(uri) ?: "application/pdf"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = LavandaPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subir Documento Médico", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Aviso informativo sobre la limitación local
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFE082))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sin Firebase Storage activo, el archivo se guarda localmente en este dispositivo.",
                            fontSize = 11.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }

                // Nombre del documento
                OutlinedTextField(
                    value = nombreText,
                    onValueChange = { nombreText = it },
                    label = { Text("Nombre del Documento") },
                    placeholder = { Text("Ej: Receta Paracetamol / Anemia Lab.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LavandaPrimary)
                )

                // Tipo de Documento
                Column {
                    Text(
                        text = "Tipo de Documento:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TipoDocumento.values().forEach { tipo ->
                            val isSelected = selectedTipo == tipo
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTipo = tipo },
                                label = { Text(tipo.etiqueta, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tipo.colorBackground,
                                    selectedLabelColor = tipo.colorText
                                )
                            )
                        }
                    }
                }

                // Fecha del Documento
                OutlinedTextField(
                    value = fechaDocText,
                    onValueChange = { fechaDocText = it },
                    label = { Text("Fecha del Documento (AAAA-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LavandaPrimary)
                )

                // Selector de Archivo (Imágenes / PDF)
                Column {
                    Text(
                        text = "Archivo o Foto del Documento:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedFileUri != null) {
                        Surface(
                            color = LavandaPrimaryLight,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = LavandaPrimary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Archivo Seleccionado", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(selectedFileUri?.lastPathSegment ?: "Documento", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { selectedFileUri = null }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar archivo", tint = Color.Gray)
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = LavandaPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Seleccionar PDF o Imagen", color = LavandaPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubir(
                        selectedTipo,
                        nombreText,
                        fechaDocText,
                        selectedFileUri?.toString() ?: "",
                        selectedMimeType
                    )
                },
                enabled = !isSubiendo && nombreText.isNotBlank() && selectedFileUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubiendo) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Subir Documento", fontWeight = FontWeight.Bold)
                }
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

package com.example.cuidafamily.ui.bitacora

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Photo
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.cuidafamily.model.BitacoraEntrada
import com.example.cuidafamily.model.EstadoAnimo
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.ui.sos.SosFloatingButton
import com.example.cuidafamily.ui.theme.GradientEnd
import com.example.cuidafamily.ui.theme.GradientStart
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.UserAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Contenedor de la Bitácora de Novedades (Muro de Actividad).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitacoraScreenContainer(
    viewModel: BitacoraViewModel,
    familyGroupId: String,
    userRole: Role,
    userName: String,
    onBackClick: () -> Unit,
    onSosClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var entradaToDelete by remember { mutableStateOf<BitacoraEntrada?>(null) }
    var previewPhotoUrl by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(familyGroupId, currentUserId) {
        viewModel.cargarBitacora(familyGroupId, currentUserId, context)
        viewModel.marcarBitacoraComoVista(familyGroupId, currentUserId)
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
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = LavandaPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bitácora de Novedades", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                    onClick = { showCreateDialog = true },
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .size(56.dp)
                        .background(fabGradient, CircleShape)
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "Nueva publicación", tint = Color.White)
                }
                SosFloatingButton(onClick = onSosClick)
            }
        }
    ) { innerPadding ->
        AppBackgroundDecorated {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LavandaPrimary)
                    }
                } else if (uiState.entradas.isEmpty()) {
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
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = LavandaPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Muro sin publicaciones",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sé el primero en compartir cómo estuvo el adulto mayor hoy, publicar una foto o dejar una nota para la familia.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publicar Novedad", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        items(uiState.entradas, key = { it.id }) { entrada ->
                            val canDelete = entrada.autorUserId == currentUserId || userRole == Role.ADMINISTRADOR_FAMILIAR

                            BitacoraFeedCard(
                                entrada = entrada,
                                canDelete = canDelete,
                                onDeleteClick = { entradaToDelete = entrada },
                                onPhotoClick = { previewPhotoUrl = entrada.fotoUrl }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Diálogo para Crear Publicación
    if (showCreateDialog) {
        CrearEntradaDialog(
            isSaving = uiState.isSaving,
            onDismiss = { showCreateDialog = false },
            onPublicar = { estadoAnimo, nota, fotoUrl ->
                viewModel.crearPublicacion(
                    familyGroupId = familyGroupId,
                    autorUserId = currentUserId,
                    autorNombre = userName,
                    autorRol = userRole,
                    estadoAnimo = estadoAnimo,
                    nota = nota,
                    fotoUrl = fotoUrl
                )
                showCreateDialog = false
            }
        )
    }

    // Diálogo de Confirmación para Eliminar Entrada
    entradaToDelete?.let { entrada ->
        AlertDialog(
            onDismissRequest = { entradaToDelete = null },
            title = { Text("Eliminar publicación", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas borrar esta novedad de la bitácora?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarPublicacion(familyGroupId, entrada.id)
                        entradaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { entradaToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Diálogo de Vista Previa de Fotografía
    previewPhotoUrl?.let { photoUrl ->
        Dialog(onDismissRequest = { previewPhotoUrl = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color.Black
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Foto ampliada de la bitácora",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                    IconButton(
                        onClick = { previewPhotoUrl = null },
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
 * Tarjeta individual de la publicación en el muro de actividad de la Bitácora.
 */
@Composable
fun BitacoraFeedCard(
    entrada: BitacoraEntrada,
    canDelete: Boolean,
    onDeleteClick: () -> Unit,
    onPhotoClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Encabezado del autor
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                UserAvatar(nombre = entrada.autorNombre, size = 42.dp)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entrada.autorNombre,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RoleBadgePill(role = entrada.autorRol)
                    }
                    Text(
                        text = formatFechaRelativa(entrada.fecha),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
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

            // Chip de Estado de Ánimo (si fue especificado)
            if (entrada.estadoAnimo != EstadoAnimo.NINGUNO) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = entrada.estadoAnimo.colorBackground,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, entrada.estadoAnimo.colorText.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = entrada.estadoAnimo.emoji,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Estado de ánimo: ${entrada.estadoAnimo.etiqueta}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = entrada.estadoAnimo.colorText
                        )
                    }
                }
            }

            // Texto / Nota de la publicación
            if (entrada.nota.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = entrada.nota,
                    fontSize = 14.sp,
                    color = Color(0xFF2C3E50),
                    lineHeight = 20.sp
                )
            }

            // Fotografía adjunta
            if (entrada.fotoUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable(onClick = onPhotoClick)
                ) {
                    AsyncImage(
                        model = entrada.fotoUrl,
                        contentDescription = "Fotografía de la bitácora",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Etiqueta visual pequeña para el rol del autor.
 */
@Composable
fun RoleBadgePill(role: Role) {
    val (label, bg, fg) = when (role) {
        Role.ADMINISTRADOR_FAMILIAR -> Triple("Admin", Color(0xFFEDE7F6), Color(0xFF673AB7))
        Role.COLABORADOR -> Triple("Familiar", Color(0xFFE3F2FD), Color(0xFF1976D2))
        Role.CUIDADOR_EXTERNO -> Triple("Cuidador", Color(0xFFE8F5E9), Color(0xFF388E3C))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Diálogo interactivo para crear una nueva publicación en la bitácora.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrearEntradaDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onPublicar: (EstadoAnimo, String, String) -> Unit
) {
    var selectedEstadoAnimo by remember { mutableStateOf(EstadoAnimo.NINGUNO) }
    var notaText by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddComment, contentDescription = null, tint = LavandaPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva Novedad en Bitácora", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Selección de Estado de Ánimo
                Column {
                    Text(
                        text = "Estado de ánimo del adulto mayor:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val opcionesAnimo = listOf(
                        EstadoAnimo.FELIZ,
                        EstadoAnimo.TRANQUILO,
                        EstadoAnimo.NORMAL,
                        EstadoAnimo.DECAIDO,
                        EstadoAnimo.IRRITADO
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        opcionesAnimo.forEach { animo ->
                            val isSelected = selectedEstadoAnimo == animo
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedEstadoAnimo = if (isSelected) EstadoAnimo.NINGUNO else animo
                                },
                                label = {
                                    Text("${animo.emoji} ${animo.etiqueta}", fontSize = 12.sp)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = animo.colorBackground,
                                    selectedLabelColor = animo.colorText
                                )
                            )
                        }
                    }
                }

                // Campo de Nota / Descripción
                OutlinedTextField(
                    value = notaText,
                    onValueChange = { notaText = it },
                    label = { Text("Nota o actualización del día") },
                    placeholder = { Text("Ej: Hoy estuvo muy alegre, paseó por el jardín y almorzó muy bien.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LavandaPrimary,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Adjuntar Fotografía
                Column {
                    Text(
                        text = "Fotografía del día (opcional):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedPhotoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF5F5F5))
                        ) {
                            AsyncImage(
                                model = selectedPhotoUri,
                                contentDescription = "Foto adjunta",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { selectedPhotoUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = LavandaPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adjuntar foto desde la galería", color = LavandaPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPublicar(
                        selectedEstadoAnimo,
                        notaText,
                        selectedPhotoUri?.toString() ?: ""
                    )
                },
                enabled = !isSaving && (notaText.isNotBlank() || selectedPhotoUri != null),
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Publicar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Función utilitaria para formatear marcas de tiempo a fechas relativas sencillas.
 */
fun formatFechaRelativa(fechaIso: String): String {
    if (fechaIso.isBlank()) return ""
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(fechaIso) ?: return fechaIso
        val diffMs = System.currentTimeMillis() - date.time
        val diffSecs = diffMs / 1000
        val diffMins = diffSecs / 60
        val diffHours = diffMins / 60
        val diffDays = diffHours / 24

        when {
            diffMins < 1 -> "Hace un momento"
            diffMins < 60 -> "Hace $diffMins min"
            diffHours < 24 -> "Hace $diffHours h"
            diffDays == 1L -> "Ayer"
            diffDays < 7 -> "Hace $diffDays días"
            else -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        fechaIso
    }
}

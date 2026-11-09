package com.example.cuidafamily.ui.document

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.cuidafamily.ui.theme.TextoPrincipalColor
import com.example.cuidafamily.ui.theme.TextoSecundarioColor
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.UserAvatar
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: migrar a Firebase Storage para compartir archivos completos sin límite de tamaño.

fun convertImageToBase64List(context: Context, uri: Uri): Triple<List<String>, Int, Int> {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return Triple(emptyList<String>(), 0, 0)
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return Triple(emptyList<String>(), 0, 0)
        inputStream.close()

        val dimensionsToTry = listOf(1280, 1000, 800)
        var finalBase64 = ""

        for (maxDim in dimensionsToTry) {
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDim || height > maxDim) {
                val max = maxOf(width, height)
                maxDim.toFloat() / max.toFloat()
            } else {
                1.0f
            }

            val resizedBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(originalBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                originalBitmap
            }

            var quality = 80
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            while (outputStream.toByteArray().size > 400 * 1024 && quality > 40) {
                outputStream.reset()
                quality -= 10
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }

            val base64Str = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            if (base64Str.length <= 850_000) {
                finalBase64 = base64Str
                break
            }
        }

        if (finalBase64.isNotBlank()) {
            Log.d("Documentos", "Copia generada: 1 página(s), ${finalBase64.length} caracteres, tipo=image")
            Triple(listOf(finalBase64), 1, 1)
        } else {
            Triple(emptyList<String>(), 1, 0)
        }
    } catch (e: Exception) {
        Log.e("Documentos", "Error en conversión de archivo de imagen: ${e.message}", e)
        Triple(emptyList<String>(), 0, 0)
    }
}

fun convertPdfToImageBase64List(context: Context, uri: Uri): Triple<List<String>, Int, Int> {
    var fileDescriptor: ParcelFileDescriptor? = null
    var pdfRenderer: PdfRenderer? = null
    try {
        fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return Triple(emptyList<String>(), 0, 0)
        pdfRenderer = PdfRenderer(fileDescriptor)

        val totalPages = pdfRenderer.pageCount
        val pagesToProcess = minOf(totalPages, 4)
        val resultList = mutableListOf<String>()
        var totalChars = 0

        for (i in 0 until pagesToProcess) {
            val page = pdfRenderer.openPage(i)
            val pageWidth = 1000
            val pageHeight = (1000f * page.height / page.width).toInt()

            val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            var quality = 75
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            while (outputStream.toByteArray().size > 200 * 1024 && quality > 35) {
                outputStream.reset()
                quality -= 10
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }

            val base64Str = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            if (totalChars + base64Str.length <= 850_000) {
                resultList.add(base64Str)
                totalChars += base64Str.length
            } else {
                break
            }
        }

        Log.d("Documentos", "Copia generada: ${resultList.size} página(s), $totalChars caracteres, tipo=pdf")
        return Triple(resultList, totalPages, resultList.size)
    } catch (e: Exception) {
        Log.e("Documentos", "Error en conversión de archivo PDF: ${e.message}", e)
        return Triple(emptyList<String>(), 0, 0)
    } finally {
        try { pdfRenderer?.close() } catch (e: Exception) {}
        try { fileDescriptor?.close() } catch (e: Exception) {}
    }
}

fun procesarArchivoParaBase64(context: Context, uri: Uri, mimeType: String): Triple<List<String>, Int, Int> {
    return if (mimeType.startsWith("image") || mimeType.contains("jpeg") || mimeType.contains("png") || mimeType.contains("jpg")) {
        convertImageToBase64List(context, uri)
    } else if (mimeType.contains("pdf")) {
        convertPdfToImageBase64List(context, uri)
    } else {
        Triple(emptyList<String>(), 0, 0)
    }
}

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
    var docToEditIndicaciones by remember { mutableStateOf<DocumentoMedico?>(null) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    var previewDocMultiPaginas by remember { mutableStateOf<DocumentoMedico?>(null) }

    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isCuidador = userRole == Role.CUIDADOR_EXTERNO

    LaunchedEffect(familyGroupId, patientId, userRole) {
        viewModel.cargarDocumentos(familyGroupId, patientId, userRole)
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
                        Text(
                            text = if (isCuidador) "Recetas Médicas" else "Documentos Médicos",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
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
                if (!isCuidador) {
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
                if (!isCuidador) {
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

                // Chips de filtro (Solo para Administrador y Colaborador)
                if (!isCuidador) {
                    FiltroChipsRow(
                        filtroActual = uiState.filtroTipo,
                        onFiltroSelected = { viewModel.setFiltroTipo(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

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
                                    text = if (isCuidador) "Sin recetas registradas" else "Sin documentos registrados",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isCuidador)
                                        "El Administrador o Colaborador aún no ha registrado recetas médicas para este paciente."
                                    else
                                        "Sube recetas, exámenes de laboratorio o radiografías para mantenerlos organizados.",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                if (!isCuidador) {
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
                                val canDelete = !isCuidador && (doc.subidoPorUserId == currentUserId || userRole == Role.ADMINISTRADOR_FAMILIAR)
                                val canEditIndicaciones = !isCuidador && doc.tipo == TipoDocumento.RECETA

                                DocumentoCard(
                                    documento = doc,
                                    isCuidador = isCuidador,
                                    canDelete = canDelete,
                                    canEditIndicaciones = canEditIndicaciones,
                                    onDeleteClick = { docToDelete = doc },
                                    onEditIndicacionesClick = { docToEditIndicaciones = doc },
                                    onCompartirConGrupoClick = {
                                        if (doc.uriLocal.isNotBlank()) {
                                            val (bList, tPag, cPag) = procesarArchivoParaBase64(context, Uri.parse(doc.uriLocal), doc.mimeType)
                                            if (bList.isNotEmpty()) {
                                                viewModel.compartirDocumentoBase64(familyGroupId, patientId, doc.id, bList, tPag, cPag)
                                            } else {
                                                Toast.makeText(context, "Abre esta receta desde el celular donde la subiste, o súbela de nuevo.", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Abre esta receta desde el celular donde la subiste, o súbela de nuevo.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onClick = {
                                        if (isCuidador) {
                                            if (doc.paginasBase64.isNotEmpty()) {
                                                previewDocMultiPaginas = doc
                                            } else {
                                                Toast.makeText(context, "Este archivo no está disponible en este celular. Revisa las indicaciones escritas de la receta.", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            var localAccessOk = false
                                            if (doc.uriLocal.isNotBlank()) {
                                                try {
                                                    val uri = Uri.parse(doc.uriLocal)
                                                    context.contentResolver.openInputStream(uri)?.close()
                                                    localAccessOk = true
                                                } catch (e: Exception) {
                                                    localAccessOk = false
                                                }
                                            }

                                            if (localAccessOk) {
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
                                            } else if (doc.paginasBase64.isNotEmpty()) {
                                                previewDocMultiPaginas = doc
                                            } else {
                                                Toast.makeText(context, "Este archivo no está disponible en este celular. Revisa las indicaciones escritas de la receta.", Toast.LENGTH_LONG).show()
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
            onSubir = { tipo, nombre, fechaDoc, uriLocal, mimeType, indicaciones, bList, tPag, cPag ->
                viewModel.subirDocumento(
                    familyGroupId = familyGroupId,
                    patientId = patientId,
                    tipo = tipo,
                    nombre = nombre,
                    fechaDocumento = fechaDoc,
                    uriLocal = uriLocal,
                    mimeType = mimeType,
                    subidoPorUserId = currentUserId,
                    subidoPorNombre = userName,
                    indicaciones = indicaciones,
                    imagenesBase64 = bList,
                    paginasTotales = tPag,
                    paginasCompartidas = cPag
                )
                showSubirDialog = false
            }
        )
    }

    // Diálogo para Editar Indicaciones de Receta
    docToEditIndicaciones?.let { doc ->
        EditarIndicacionesDialog(
            indicacionesActuales = doc.indicaciones,
            onDismiss = { docToEditIndicaciones = null },
            onGuardar = { nuevasIndicaciones ->
                viewModel.actualizarIndicaciones(familyGroupId, patientId, doc.id, nuevasIndicaciones)
                docToEditIndicaciones = null
            }
        )
    }

    // Diálogo de Confirmación para Eliminar Documento
    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = { Text("Eliminar documento", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("¿Estás seguro de que deseas eliminar '${doc.nombre}'?", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarDocumento(familyGroupId, patientId, doc.id)
                        docToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { docToDelete = null }, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Vista Previa de Imagen por URI Local
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

    // Visor Multi-Páginas Base64
    previewDocMultiPaginas?.let { doc ->
        VisorMultiPaginasDialog(
            paginasBase64 = doc.paginasBase64,
            titulo = doc.nombre,
            paginasTotales = doc.paginasTotales,
            paginasCompartidas = doc.paginasCompartidas,
            onDismiss = { previewDocMultiPaginas = null }
        )
    }
}

@Composable
fun VisorMultiPaginasDialog(
    paginasBase64: List<String>,
    titulo: String,
    paginasTotales: Int,
    paginasCompartidas: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { paginasBase64.size })

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            color = Color.Black
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titulo,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                if (paginasTotales > paginasCompartidas && paginasCompartidas > 0) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Se comparten $paginasCompartidas de $paginasTotales páginas del documento.",
                            color = Color(0xFFE65100),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val base64Str = paginasBase64[pageIdx]
                        val bitmap = remember(base64Str) {
                            try {
                                val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Página ${pageIdx + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Error cargando página ${pageIdx + 1}", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                if (paginasBase64.size > 1) {
                    Text(
                        text = "Página ${pagerState.currentPage + 1} de ${paginasBase64.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

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
 * Tarjeta individual para cada documento médico o receta.
 */
@Composable
fun DocumentoCard(
    documento: DocumentoMedico,
    isCuidador: Boolean = false,
    canDelete: Boolean,
    canEditIndicaciones: Boolean = false,
    onDeleteClick: () -> Unit,
    onEditIndicacionesClick: () -> Unit = {},
    onCompartirConGrupoClick: () -> Unit = {},
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(
                        text = documento.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextoPrincipalColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = documento.tipo.colorBackground,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = documento.tipo.etiqueta,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = documento.tipo.colorText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Badge de estado de compartición en el grupo
                        if (documento.estaCompartidoConGrupo) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (documento.paginasTotales > documento.paginasCompartidas && documento.paginasCompartidas > 0)
                                        "✅ Compartido (${documento.paginasCompartidas}/${documento.paginasTotales} pág.)"
                                    else
                                        "✅ Compartido con el grupo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🔒 Solo en el celular de quien lo subió",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF616161),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
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

            // Indicaciones Destacadas para Recetas Médicas
            if (documento.tipo == TipoDocumento.RECETA && documento.indicaciones.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFE082)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "💊 Medicamentos e Indicaciones:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFE65100)
                            )
                            if (canEditIndicaciones) {
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = onEditIndicacionesClick,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar Indicaciones",
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = documento.indicaciones,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextoPrincipalColor,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else if (canEditIndicaciones && documento.tipo == TipoDocumento.RECETA) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onEditIndicacionesClick,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = LavandaPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Agregar Dosis / Indicaciones Escritas", fontSize = 12.sp, color = LavandaPrimary, fontWeight = FontWeight.Bold)
                }
            }

            // Botón "Compartir con el grupo" para documentos sin copia Base64
            if (!documento.estaCompartidoConGrupo && !isCuidador) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCompartirConGrupoClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = LavandaPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartir con el grupo", fontSize = 12.sp, color = LavandaPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Fecha doc: ${documento.fechaDocumento} • Subido por: ${documento.subidoPorNombre}",
                fontSize = 12.sp,
                color = TextoSecundarioColor
            )
        }
    }
}

@Composable
fun EditarIndicacionesDialog(
    indicacionesActuales: String,
    onDismiss: () -> Unit,
    onGuardar: (String) -> Unit
) {
    var indicacionesText by remember { mutableStateOf(indicacionesActuales) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Indicaciones de Receta", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Escribe o modifica los medicamentos, dosis y horarios prescritos:", fontSize = 13.sp, color = Color.DarkGray)
                OutlinedTextField(
                    value = indicacionesText,
                    onValueChange = { indicacionesText = it },
                    label = { Text("Medicamentos, dosis y horarios") },
                    placeholder = { Text("Ej: Losartán 50 mg: 1 tableta cada mañana (8:00 AM)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGuardar(indicacionesText) },
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar Indicaciones", fontWeight = FontWeight.Bold)
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

/**
 * Diálogo para formulario de registro y selección de archivo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubirDocumentoDialog(
    isSubiendo: Boolean,
    onDismiss: () -> Unit,
    onSubir: (TipoDocumento, String, String, String, String, String, List<String>, Int, Int) -> Unit
) {
    val context = LocalContext.current

    var selectedTipo by remember { mutableStateOf(TipoDocumento.RECETA) }
    var nombreText by remember { mutableStateOf("") }
    var fechaDocText by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var indicacionesText by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMimeType by remember { mutableStateOf("") }
    var isProcessingFile by remember { mutableStateOf(false) }

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
                // Nombre del documento
                OutlinedTextField(
                    value = nombreText,
                    onValueChange = { nombreText = it },
                    label = { Text("Nombre del Documento") },
                    placeholder = { Text("Ej: Receta Losartán / Examen Sangre") },
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
                        TipoDocumento.entries.forEach { tipo ->
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

                // Campo de Indicaciones/Medicamentos escrito cuando es Receta
                if (selectedTipo == TipoDocumento.RECETA) {
                    Column {
                        OutlinedTextField(
                            value = indicacionesText,
                            onValueChange = { indicacionesText = it },
                            label = { Text("Medicamentos, dosis y horarios") },
                            placeholder = { Text("Ej: Losartán 50 mg: 1 tableta cada mañana (8:00 AM)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LavandaPrimary)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "💡 Las indicaciones permiten que los cuidadores conozcan las dosis prescritas directamente en la app sin descargar archivos.",
                            fontSize = 11.sp,
                            color = TextoSecundarioColor,
                            modifier = Modifier.padding(start = 4.dp)
                        )
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

                if (isProcessingFile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = LavandaPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Preparando archivo para compartir…", fontSize = 13.sp, color = LavandaPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isProcessingFile = true
                    val (base64List, totalPag, compPag) = if (selectedFileUri != null) {
                        procesarArchivoParaBase64(context, selectedFileUri!!, selectedMimeType)
                    } else {
                        Triple(emptyList<String>(), 0, 0)
                    }
                    isProcessingFile = false

                    if (selectedFileUri != null && base64List.isEmpty()) {
                        Toast.makeText(context, "No se pudo compartir el archivo con el grupo. Escribe las indicaciones en el campo de texto para que el cuidador las vea.", Toast.LENGTH_LONG).show()
                    }

                    onSubir(
                        selectedTipo,
                        nombreText,
                        fechaDocText,
                        selectedFileUri?.toString() ?: "",
                        selectedMimeType,
                        indicacionesText,
                        base64List,
                        totalPag,
                        compPag
                    )
                },
                enabled = !isSubiendo && !isProcessingFile && nombreText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubiendo || isProcessingFile) {
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

package com.example.cuidafamily.ui.auditoria

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.cuidafamily.model.AccionAuditoria
import com.example.cuidafamily.model.EntidadAuditoria
import com.example.cuidafamily.model.RegistroAuditoria
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.ui.theme.*
import com.example.cuidafamily.ui.util.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

fun traducirRolEspanol(rolStr: String): String {
    return when (rolStr) {
        "ADMINISTRADOR_FAMILIAR" -> "Administrador"
        "COLABORADOR" -> "Familiar Colaborador"
        "CUIDADOR_EXTERNO" -> "Cuidador Externo"
        else -> rolStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditoriaScreenContainer(
    viewModel: AuditoriaViewModel,
    familyGroupId: String,
    userRole: Role?,
    userName: String,
    onBackClick: () -> Unit,
    onRedirectHome: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val esAdmin = userRole == Role.ADMINISTRADOR_FAMILIAR

    // Requerimiento 04: Redirigir si el rol no es ADMINISTRADOR_FAMILIAR
    LaunchedEffect(userRole, state.sinAcceso) {
        if (!esAdmin || state.sinAcceso) {
            Log.e("Auditoria", "Acceso denegado a Auditoría para rol=$userRole. Redirigiendo al Home.")
            Toast.makeText(context, "Acceso restringido únicamente para el Administrador Familiar.", Toast.LENGTH_SHORT).show()
            onRedirectHome()
        }
    }

    LaunchedEffect(familyGroupId, userRole) {
        if (esAdmin) {
            viewModel.iniciar(familyGroupId, userRole)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.limpiarMensajes()
        }
    }

    var showFechaDesdeDialog by remember { mutableStateOf(false) }
    var showFechaHastaDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = LavandaPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reportes e Historial",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { exportarAuditoriaCsv(context, state.registrosFiltrados) }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Exportar CSV", tint = LavandaPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        AppBackgroundDecorated {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Tarjeta de Resumen de Movimientos
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resumen de Movimientos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextoSecundarioColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${state.totalMovimientos} movimientos registrados",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val countCrear = state.conteoPorAccion[AccionAuditoria.CREAR] ?: 0
                            val countMod = state.conteoPorAccion[AccionAuditoria.MODIFICAR] ?: 0
                            val countElim = state.conteoPorAccion[AccionAuditoria.ELIMINAR] ?: 0

                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                                Text("🟢 Creó: $countCrear", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }

                            Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(8.dp)) {
                                Text("🟠 Modificó: $countMod", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }

                            Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp)) {
                                Text("🔴 Eliminó: $countElim", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }

                // Sección de Filtros
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Selector de Rango de Fechas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showFechaDesdeDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Desde: ${state.fechaDesde}", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { showFechaHastaDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Hasta: ${state.fechaHasta}", fontSize = 12.sp)
                            }
                        }

                        // Filter Chips por Entidad
                        Column {
                            Text("Módulo / Entidad:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoSecundarioColor)
                            Spacer(modifier = Modifier.height(2.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                item {
                                    FilterChip(
                                        selected = state.filtroEntidad == null,
                                        onClick = { viewModel.setFiltroEntidad(null) },
                                        label = { Text("Todos", fontSize = 11.sp) }
                                    )
                                }
                                items(EntidadAuditoria.values()) { entidad ->
                                    FilterChip(
                                        selected = state.filtroEntidad == entidad,
                                        onClick = { viewModel.setFiltroEntidad(entidad) },
                                        label = { Text(entidad.etiqueta, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // Filter Chips por Acción
                        Column {
                            Text("Acción:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoSecundarioColor)
                            Spacer(modifier = Modifier.height(2.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                item {
                                    FilterChip(
                                        selected = state.filtroAccion == null,
                                        onClick = { viewModel.setFiltroAccion(null) },
                                        label = { Text("Todas", fontSize = 11.sp) }
                                    )
                                }
                                items(AccionAuditoria.values()) { accion ->
                                    FilterChip(
                                        selected = state.filtroAccion == accion,
                                        onClick = { viewModel.setFiltroAccion(accion) },
                                        label = { Text(accion.etiqueta, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // Selector de Persona
                        if (state.miembrosGrupo.isNotEmpty()) {
                            Column {
                                Text("Persona:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoSecundarioColor)
                                Spacer(modifier = Modifier.height(2.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    item {
                                        FilterChip(
                                            selected = state.filtroUserId.isBlank(),
                                            onClick = { viewModel.setFiltroUserId("") },
                                            label = { Text("Todas", fontSize = 11.sp) }
                                        )
                                    }
                                    items(state.miembrosGrupo, key = { it.member.userId }) { m ->
                                        FilterChip(
                                            selected = state.filtroUserId == m.member.userId,
                                            onClick = { viewModel.setFiltroUserId(m.member.userId) },
                                            label = { Text(m.userName, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LavandaPrimary)
                    }
                } else if (state.registrosFiltrados.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HistoryToggleOff, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No hay movimientos en este periodo", color = TextoPrincipalColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Prueba ajustando el rango de fechas o los filtros.", fontSize = 13.sp, color = TextoSecundarioColor, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    // Agrupación por día en la Línea de Tiempo
                    val gruposPorDia = remember(state.registrosFiltrados) {
                        state.registrosFiltrados.groupBy { reg ->
                            if (reg.fecha > 0L) {
                                val date = Instant.ofEpochMilli(reg.fecha)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                formatFechaEspanolLarga(date)
                            } else {
                                "Fecha no registrada"
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        gruposPorDia.forEach { (diaHeader, itemsDia) ->
                            item {
                                Surface(
                                    color = LavandaPrimaryLight,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = diaHeader,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LavandaPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            items(itemsDia, key = { it.id }) { reg ->
                                ItemRegistroAuditoriaCard(reg = reg)
                            }
                        }

                        if (state.todosLosRegistrosFirestore.size >= state.limiteFirestore) {
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.cargarMas() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                ) {
                                    Text("Cargar más movimientos", fontWeight = FontWeight.Bold, color = LavandaPrimary)
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showFechaDesdeDialog) {
        SelectorFechaDialog(
            fechaInicial = state.fechaDesde,
            titulo = "Fecha Desde",
            onFechaSeleccionada = { fecha ->
                viewModel.setFechaDesde(fecha)
                showFechaDesdeDialog = false
            },
            onCancelar = { showFechaDesdeDialog = false }
        )
    }

    if (showFechaHastaDialog) {
        SelectorFechaDialog(
            fechaInicial = state.fechaHasta,
            titulo = "Fecha Hasta",
            fechaMinima = state.fechaDesde,
            onFechaSeleccionada = { fecha ->
                viewModel.setFechaHasta(fecha)
                showFechaHastaDialog = false
            },
            onCancelar = { showFechaHastaDialog = false }
        )
    }
}

@Composable
fun ItemRegistroAuditoriaCard(reg: RegistroAuditoria) {
    var expandido by remember { mutableStateOf(false) }

    val (colorIcono, iconoAccion) = when (reg.accion) {
        AccionAuditoria.CREAR -> Pair(Color(0xFF2E7D32), Icons.Default.AddCircle)
        AccionAuditoria.MODIFICAR -> Pair(Color(0xFFE65100), Icons.Default.Edit)
        AccionAuditoria.ELIMINAR -> Pair(Color(0xFFC62828), Icons.Default.Delete)
    }

    val horaStr = remember(reg.fecha) {
        if (reg.fecha > 0L) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(reg.fecha))
        } else "--:--"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = colorIcono.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(iconoAccion, contentDescription = null, tint = colorIcono, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${reg.userNombre} (${traducirRolEspanol(reg.userRol)})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipalColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${reg.accion.verboEspanol} ${reg.descripcion}",
                        fontSize = 13.sp,
                        color = TextoPrincipalColor
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = horaStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoSecundarioColor
                )
            }

            if (reg.detalleCambios.isNotBlank()) {
                AnimatedVisibility(visible = expandido) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Detalle de cambios:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextoSecundarioColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reg.detalleCambios,
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                if (!expandido) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Toca para ver detalles ➔",
                        fontSize = 10.sp,
                        color = LavandaPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun exportarAuditoriaCsv(context: Context, registros: List<RegistroAuditoria>) {
    if (registros.isEmpty()) {
        Toast.makeText(context, "No hay registros para exportar.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("Fecha y Hora,Persona,Rol,Entidad,Acción,Descripción,Detalle\n")

        registros.forEach { r ->
            val fechaStr = if (r.fecha > 0L) sdf.format(Date(r.fecha)) else ""
            val descLimpia = r.descripcion.replace("\"", "\"\"").replace("\n", " ")
            val detLimpia = r.detalleCambios.replace("\"", "\"\"").replace("\n", " ")
            val rolEspanol = traducirRolEspanol(r.userRol)
            sb.append("\"$fechaStr\",\"${r.userNombre}\",\"$rolEspanol\",\"${r.entidad.etiqueta}\",\"${r.accion.etiqueta}\",\"$descLimpia\",\"$detLimpia\"\n")
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "CuidaFamily_Auditoria_$timeStamp.csv"
        val csvContent = sb.toString()

        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            insertUri?.also { u ->
                resolver.openOutputStream(u)?.use { os ->
                    os.write(csvContent.toByteArray(Charsets.UTF_8))
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { os ->
                os.write(csvContent.toByteArray(Charsets.UTF_8))
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        if (uri != null) {
            Toast.makeText(context, "Reporte exportado a Descargas: $fileName", Toast.LENGTH_LONG).show()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir Reporte de Auditoría (CSV)"))
        } else {
            Toast.makeText(context, "No se pudo generar el reporte CSV.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("AuditoriaExport", "Error exportando CSV", e)
        Toast.makeText(context, "Error al exportar CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

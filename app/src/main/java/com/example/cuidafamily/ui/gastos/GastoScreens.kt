package com.example.cuidafamily.ui.gastos

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.cuidafamily.model.Gasto
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.ui.theme.*
import com.example.cuidafamily.ui.util.*
import com.example.cuidafamily.util.ComprobanteProcessor
import com.example.cuidafamily.util.Permisos
import com.example.cuidafamily.util.formatearSoles
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastoScreenContainer(
    viewModel: GastoViewModel,
    pagoViewModel: PagoViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    familyGroupId: String,
    currentUserId: String,
    userName: String,
    userRole: Role?,
    onBackClick: () -> Unit,
    onRedirectHome: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val pagoState by pagoViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }

    var showGastoDialog by remember { mutableStateOf(false) }
    var gastoToEdit by remember { mutableStateOf<Gasto?>(null) }
    var gastoToDelete by remember { mutableStateOf<Gasto?>(null) }
    var gastoToDetail by remember { mutableStateOf<Gasto?>(null) }

    val acceso = Permisos.puedeVerFinanzas(userRole)
    Log.d("Gastos", "rol=$userRole, acceso=$acceso")

    LaunchedEffect(userRole, state.sinAcceso) {
        if (!acceso || state.sinAcceso) {
            Log.e("Gastos", "Acceso denegado a Finanzas para rol=$userRole. Redirigiendo al Home.")
            Toast.makeText(context, "No tienes permiso para acceder al módulo financiero.", Toast.LENGTH_SHORT).show()
            onRedirectHome()
        }
    }

    LaunchedEffect(familyGroupId, currentUserId, userRole) {
        if (acceso) {
            viewModel.iniciar(familyGroupId, currentUserId, userRole)
            pagoViewModel.iniciar(familyGroupId, currentUserId, userName, userRole, context)
        }
    }

    LaunchedEffect(state.errorMessage, state.successMessage, pagoState.errorMessage, pagoState.successMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.limpiarMensajes()
        }
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.limpiarMensajes()
        }
        pagoState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            pagoViewModel.limpiarMensajes()
        }
        pagoState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            pagoViewModel.limpiarMensajes()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = LavandaPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Módulo Financiero / Finanzas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = {
                            exportarGastosCsv(context, state.gastos)
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Exportar CSV", tint = LavandaPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (acceso && selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        gastoToEdit = null
                        showGastoDialog = true
                    },
                    containerColor = LavandaPrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Registrar Gasto")
                }
            }
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
                // TabRow del Módulo Financiero
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = LavandaPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Gastos", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Pagos a Cuidadores", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Payments, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTab == 0) {
                    // TAB 0: GASTOS
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Resumen Total de Gastos", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextoSecundarioColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatearSoles(state.totalGastos),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Requerimiento 6: Línea aparte "Pagos a cuidadores del mes: S/ X", calculada desde pagos (NO crees gastos duplicados)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFF3E5F5),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp), tint = LavandaPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pagos a cuidadores del mes: ${formatearSoles(pagoState.totalPagadoMes)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LavandaPrimary
                                    )
                                }
                            }

                            if (state.gastosPorCategoria.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Gastos por Categoría:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor)
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.gastosPorCategoria.toList()) { (cat, montoCat) ->
                                        Surface(
                                            color = LavandaPrimaryLight,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "$cat: ${formatearSoles(montoCat)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LavandaPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LavandaPrimary)
                        }
                    } else if (state.gastos.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Aún no hay gastos registrados", color = TextoPrincipalColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Toca el botón '+' para registrar el primer gasto del grupo.", fontSize = 13.sp, color = TextoSecundarioColor, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.gastos, key = { it.id }) { gasto ->
                                GastoCard(
                                    gasto = gasto,
                                    currentUserId = currentUserId,
                                    userName = userName,
                                    userRole = userRole,
                                    onClick = { gastoToDetail = gasto },
                                    onEditar = {
                                        gastoToEdit = gasto
                                        showGastoDialog = true
                                    },
                                    onEliminar = {
                                        gastoToDelete = gasto
                                    },
                                    onReclamarComoMio = {
                                        viewModel.reclamarGastoComoMio(familyGroupId, gasto.id, currentUserId, userName, userRole)
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                } else {
                    // TAB 1: PAGOS A CUIDADORES
                    PagosTabContent(
                        viewModel = pagoViewModel,
                        familyGroupId = familyGroupId,
                        currentUserId = currentUserId,
                        currentUserName = userName,
                        userRole = userRole
                    )
                }
            }
        }
    }

    if (showGastoDialog || gastoToEdit != null) {
        CrearOEditarGastoDialog(
            initialGasto = gastoToEdit,
            currentUserId = currentUserId,
            currentUserName = userName,
            viewModel = viewModel,
            isSaving = state.isSaving,
            isProcessingComprobante = state.isProcessingComprobante,
            comprobanteMsg = state.comprobanteProcessingMsg,
            onDismiss = {
                showGastoDialog = false
                gastoToEdit = null
            },
            onGuardar = { gasto ->
                viewModel.crearOEditarGasto(familyGroupId, gasto, userRole)
                showGastoDialog = false
                gastoToEdit = null
            }
        )
    }

    if (gastoToDelete != null) {
        AlertDialog(
            onDismissRequest = { gastoToDelete = null },
            title = { Text("Eliminar gasto", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("¿Deseas eliminar el registro de \"${gastoToDelete?.concepto}\" (${formatearSoles(gastoToDelete?.monto ?: 0.0)})?", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        val target = gastoToDelete
                        if (target != null) {
                            viewModel.eliminarGasto(familyGroupId, target, currentUserId, userRole)
                        }
                        gastoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RojoAlerta),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { gastoToDelete = null }, shape = RoundedCornerShape(12.dp)) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (gastoToDetail != null) {
        DetalleGastoDialog(
            gasto = gastoToDetail!!,
            currentUserId = currentUserId,
            onDismiss = { gastoToDetail = null }
        )
    }
}

@Composable
fun GastoCard(
    gasto: Gasto,
    currentUserId: String,
    userName: String,
    userRole: Role?,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onReclamarComoMio: () -> Unit
) {
    val realUid = FirebaseAuth.getInstance().currentUser?.uid ?: currentUserId
    val autorUid = gasto.registradoPorUserId
    val esSinPropietario = autorUid.isBlank()
    val puedeModificar = (!esSinPropietario && autorUid == realUid)

    // Log requerido
    Log.d("Gastos", "uid=$realUid, autor=$autorUid, puedeModificar=$puedeModificar")

    val tieneComprobanteCompartido = gasto.listaComprobantes.isNotEmpty()
    val tieneComprobanteLocal = gasto.comprobanteUrl.isNotBlank()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = gasto.concepto,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipalColor,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatearSoles(gasto.monto),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = LavandaPrimaryLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = gasto.categoria.ifBlank { "General" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LavandaPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (gasto.fecha.isNotBlank()) formatFechaEspanolCorta(LocalDate.parse(gasto.fecha)) else "",
                    fontSize = 12.sp,
                    color = TextoSecundarioColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Requerimiento 1: Si no es el autor, muestra ícono de candado + "Registrado por [nombre]" (14sp+) o "Sin propietario"
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!puedeModificar) {
                    Icon(Icons.Default.Lock, contentDescription = "Solo lectura", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (esSinPropietario) "Sin propietario" else "Registrado por ${gasto.registradoPorNombre.ifBlank { "Familiar" }}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (esSinPropietario) Color.Gray else Color.DarkGray
                )
            }

            // Badges de comprobante
            if (tieneComprobanteCompartido) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF81C784))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Comprobante compartido (${gasto.listaComprobantes.size} pág)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
            } else if (tieneComprobanteLocal) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFEEEEEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Solo en este celular", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (puedeModificar) {
                    TextButton(onClick = onEditar) {
                        Text("Editar", fontWeight = FontWeight.Bold, color = LavandaPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onEliminar) {
                        Text("Eliminar", fontWeight = FontWeight.Bold, color = RojoAlerta)
                    }
                } else if (esSinPropietario && userRole == Role.ADMINISTRADOR_FAMILIAR) {
                    Button(
                        onClick = onReclamarComoMio,
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Reclamar como mío", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Text("Solo lectura", fontSize = 12.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearOEditarGastoDialog(
    initialGasto: Gasto?,
    currentUserId: String,
    currentUserName: String,
    viewModel: GastoViewModel,
    isSaving: Boolean,
    isProcessingComprobante: Boolean,
    comprobanteMsg: String?,
    onDismiss: () -> Unit,
    onGuardar: (Gasto) -> Unit
) {
    val context = LocalContext.current

    var concepto by remember { mutableStateOf(initialGasto?.concepto ?: "") }
    var montoText by remember { mutableStateOf(initialGasto?.monto?.takeIf { it > 0 }?.let { DecimalFormat("0.00", DecimalFormatSymbols(Locale.US)).format(it) } ?: "") }
    var categoria by remember { mutableStateOf(initialGasto?.categoria ?: "General") }
    var fechaIso by remember { mutableStateOf(initialGasto?.fecha?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString()) }
    var comprobanteUrl by remember { mutableStateOf(initialGasto?.comprobanteUrl ?: "") }

    var comprobanteBase64 by remember { mutableStateOf(initialGasto?.comprobanteBase64 ?: "") }
    var comprobantesBase64 by remember { mutableStateOf(initialGasto?.comprobantesBase64 ?: emptyList()) }

    var errorMonto by remember { mutableStateOf<String?>(null) }
    var errorConcepto by remember { mutableStateOf<String?>(null) }

    var showFechaDialog by remember { mutableStateOf(false) }
    var showSelectorSheet by remember { mutableStateOf(false) }

    val tempCamaraFile = remember { File.createTempFile("comp_cam_", ".jpg", context.cacheDir) }
    val tempCamaraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempCamaraFile)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.procesarComprobante(context, tempCamaraUri, "camara") { result ->
                comprobanteUrl = tempCamaraUri.toString()
                if (result.base64List.isNotEmpty()) {
                    comprobantesBase64 = result.base64List
                    comprobanteBase64 = result.base64List.first()
                }
            }
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.procesarComprobante(context, selectedUri, "galeria") { result ->
                comprobanteUrl = selectedUri.toString()
                if (result.base64List.isNotEmpty()) {
                    comprobantesBase64 = result.base64List
                    comprobanteBase64 = result.base64List.first()
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.procesarComprobante(context, selectedUri, "documento") { result ->
                comprobanteUrl = selectedUri.toString()
                if (result.base64List.isNotEmpty()) {
                    comprobantesBase64 = result.base64List
                    comprobanteBase64 = result.base64List.first()
                }
            }
        }
    }

    val categoriasOptions = listOf("General", "Farmacia", "Médicos", "Alimentación", "Transporte", "Servicios", "Otros")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initialGasto == null) "Registrar Nuevo Gasto" else "Editar Gasto",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = concepto,
                    onValueChange = {
                        concepto = it
                        if (it.isNotBlank()) errorConcepto = null
                    },
                    label = { Text("Concepto / Descripción") },
                    isError = errorConcepto != null,
                    supportingText = { errorConcepto?.let { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = montoText,
                    onValueChange = { input ->
                        if (input.isBlank() || input.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                            montoText = input
                            val valMonto = input.toDoubleOrNull() ?: 0.0
                            if (input.isNotBlank() && valMonto > 0.0) {
                                errorMonto = null
                            }
                        }
                    },
                    label = { Text("Monto") },
                    prefix = { Text("S/ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMonto != null,
                    supportingText = { errorMonto?.let { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Column {
                    Text("Categoría:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextoPrincipalColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categoriasOptions) { cat ->
                            val isSel = categoria == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { categoria = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showFechaDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Fecha: ${formatFechaEspanolCorta(LocalDate.parse(fechaIso))}", fontSize = 13.sp)
                }

                Column {
                    Text("Comprobante de compra:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextoPrincipalColor)
                    Spacer(modifier = Modifier.height(6.dp))

                    val tieneAdjunto = comprobantesBase64.isNotEmpty() || comprobanteBase64.isNotBlank() || comprobanteUrl.isNotBlank()

                    if (isProcessingComprobante) {
                        Surface(
                            color = LavandaPrimaryLight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = LavandaPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(comprobanteMsg ?: "Preparando comprobante…", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary)
                            }
                        }
                    } else if (tieneAdjunto) {
                        Surface(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.LightGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = LavandaPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (comprobantesBase64.isNotEmpty()) "Comprobante adjunto (${comprobantesBase64.size} pág)" else "Comprobante adjunto",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextoPrincipalColor
                                    )
                                }

                                Row {
                                    TextButton(onClick = { showSelectorSheet = true }) {
                                        Text("Cambiar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary)
                                    }
                                    TextButton(onClick = {
                                        comprobanteUrl = ""
                                        comprobanteBase64 = ""
                                        comprobantesBase64 = emptyList()
                                    }) {
                                        Text("Quitar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RojoAlerta)
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { showSelectorSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, LavandaPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = LavandaPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Adjuntar comprobante (Foto o Archivo)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LavandaPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val montoVal = montoText.toDoubleOrNull() ?: 0.0
                            var valid = true

                            if (concepto.isBlank()) {
                                errorConcepto = "El concepto es obligatorio."
                                valid = false
                            }

                            if (montoText.isBlank() || montoVal <= 0.0) {
                                errorMonto = "El monto es obligatorio y debe ser mayor a cero."
                                valid = false
                            }

                            if (valid) {
                                val finalGasto = (initialGasto ?: Gasto()).copy(
                                    concepto = concepto.trim(),
                                    monto = montoVal,
                                    categoria = categoria,
                                    fecha = fechaIso,
                                    comprobanteUrl = comprobanteUrl.trim(),
                                    comprobanteBase64 = comprobanteBase64,
                                    comprobantesBase64 = comprobantesBase64,
                                    registradoPorUserId = initialGasto?.registradoPorUserId?.takeIf { it.isNotBlank() } ?: currentUserId,
                                    registradoPorNombre = initialGasto?.registradoPorNombre?.takeIf { it.isNotBlank() } ?: currentUserName
                                )
                                onGuardar(finalGasto)
                            }
                        },
                        enabled = !isSaving && !isProcessingComprobante,
                        colors = ButtonDefaults.buttonColors(containerColor = LavandaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showFechaDialog) {
        SelectorFechaDialog(
            fechaInicial = try { LocalDate.parse(fechaIso) } catch (e: Exception) { LocalDate.now() },
            titulo = "Fecha del gasto",
            onFechaSeleccionada = { fechaIso = it.toString(); showFechaDialog = false },
            onCancelar = { showFechaDialog = false }
        )
    }

    if (showSelectorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSelectorSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Adjuntar comprobante", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        showSelectorSheet = false
                        takePictureLauncher.launch(tempCamaraUri)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("📷 Tomar foto", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor)
                    }
                }

                Button(
                    onClick = {
                        showSelectorSheet = false
                        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("🖼️ Elegir de la galería", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor)
                    }
                }

                Button(
                    onClick = {
                        showSelectorSheet = false
                        openDocumentLauncher.launch(arrayOf("application/pdf", "image/*"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("📄 Elegir archivo (PDF o imagen)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextoPrincipalColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleGastoDialog(
    gasto: Gasto,
    currentUserId: String = "",
    onDismiss: () -> Unit
) {
    val realUid = FirebaseAuth.getInstance().currentUser?.uid ?: currentUserId
    val esAutor = gasto.registradoPorUserId == realUid
    val lista = gasto.listaComprobantes
    val totalPaginas = lista.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (lista.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { totalPaginas })

                    Column(modifier = Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(gasto.concepto, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = "${formatearSoles(gasto.monto)} · ${gasto.fecha}${if (!esAutor) " · Solo lectura" else ""}",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.7f))
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { pageIdx ->
                                val b64 = lista[pageIdx]
                                val bitmap = remember(b64) { ComprobanteProcessor.decodeBase64ToBitmap(b64) }

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Página ${pageIdx + 1}",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Error cargando página ${pageIdx + 1}", color = Color.White)
                                    }
                                }
                            }

                            if (totalPaginas > 1) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} de $totalPaginas",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (gasto.comprobanteUrl.isNotBlank()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(gasto.concepto, color = Color.White, fontWeight = FontWeight.Bold)
                                    if (!esAutor) {
                                        Text("Solo lectura", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                        )

                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = gasto.comprobanteUrl,
                                contentDescription = "Comprobante local",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Sin comprobante adjunto", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onDismiss) {
                                Text("Cerrar")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun exportarGastosCsv(context: Context, gastos: List<Gasto>) {
    if (gastos.isEmpty()) {
        Toast.makeText(context, "No hay gastos para exportar.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val dfPen = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
        val sb = StringBuilder()
        sb.append("ID,Fecha,Concepto,Monto (PEN),Moneda,Categoria,RegistradoPor\n")
        gastos.forEach { g ->
            val montoPen = dfPen.format(g.monto)
            sb.append("\"${g.id}\",\"${g.fecha}\",\"${g.concepto}\",$montoPen,\"PEN\",\"${g.categoria}\",\"${g.registradoPorNombre}\"\n")
        }

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Exportar Registro de Gastos (CSV - PEN)")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar CSV: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

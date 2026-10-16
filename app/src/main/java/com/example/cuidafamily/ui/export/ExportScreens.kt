package com.example.cuidafamily.ui.export

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cuidafamily.model.Role
import com.example.cuidafamily.repository.BitacoraRepository
import com.example.cuidafamily.repository.PatientRepository
import com.example.cuidafamily.repository.Result
import com.example.cuidafamily.ui.sos.SosFloatingButton
import com.example.cuidafamily.ui.theme.LavandaPrimary
import com.example.cuidafamily.ui.util.AppBackgroundDecorated
import com.example.cuidafamily.ui.util.UserAvatar
import com.example.cuidafamily.util.ExportHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// TODO: migrar a Firebase Storage y FCM + Cloud Functions (plan Blaze) para notificaciones y exportación remota.

enum class FormatoExportacion {
    PDF,
    CSV
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportarHistorialScreen(
    familyGroupId: String,
    userRole: Role,
    userName: String,
    onBackClick: () -> Unit,
    onSosClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFormato by remember { mutableStateOf(FormatoExportacion.PDF) }
    var isGenerando by remember { mutableStateOf(false) }

    val gradientStart = Color(0xFFFF6B9D)
    val gradientEnd = Color(0xFF7C4DFF)
    val selectedBrush = Brush.horizontalGradient(listOf(gradientStart, gradientEnd))

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Exportar Historial", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
            SosFloatingButton(onClick = onSosClick)
        }
    ) { innerPadding ->
        AppBackgroundDecorated {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (userRole == Role.CUIDADOR_EXTERNO) {
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
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Acceso Restringido",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "La exportación del historial médico está reservada únicamente para el Administrador Familiar y Colaboradores.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = onBackClick) {
                                Text("Volver")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, LavandaPrimary.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFF3E8FF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = LavandaPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Genera informes para médicos, aseguradoras o archivo personal",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Exporta la ficha médica, alergias y bitácora en PDF o tabla CSV/Excel.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF555555)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "1. Formato de Exportación",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FormatoSeleccionableCard(
                            titulo = "PDF Clínico",
                            subtitulo = "Resumen formateado para imprimir",
                            descripcion = "Documento bien estructurado para médicos y controles. Incluye ficha médica, alergias y últimas publicaciones de la bitácora.",
                            chips = listOf("Datos de la ficha", "Alergias", "Bitácora reciente"),
                            icon = Icons.AutoMirrored.Filled.Article,
                            isSelected = selectedFormato == FormatoExportacion.PDF,
                            onClick = { selectedFormato = FormatoExportacion.PDF },
                            selectedBrush = selectedBrush
                        )

                        FormatoSeleccionableCard(
                            titulo = "Excel / CSV",
                            subtitulo = "Datos estructurados en tabla",
                            descripcion = "Archivo en formato de tabla (.csv) compatible con Microsoft Excel y hojas de cálculo. Ideal para análisis o archivo digital.",
                            chips = listOf("Compatible con Excel", "Apto para análisis"),
                            icon = Icons.Default.GridOn,
                            isSelected = selectedFormato == FormatoExportacion.CSV,
                            onClick = { selectedFormato = FormatoExportacion.CSV },
                            selectedBrush = selectedBrush
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                isGenerando = true
                                coroutineScope.launch {
                                    try {
                                        val patientResult = PatientRepository.obtenerPatientDelGrupo(familyGroupId)
                                        val patient = if (patientResult is Result.Success && patientResult.data != null) {
                                            patientResult.data
                                        } else {
                                            Toast.makeText(context, "No se encontró el paciente asignado.", Toast.LENGTH_SHORT).show()
                                            isGenerando = false
                                            return@launch
                                        }

                                        val alergiasResult = PatientRepository.obtenerAlergiasDelPatient(familyGroupId, patient.id)
                                        val alergias = if (alergiasResult is Result.Success) alergiasResult.data else emptyList()

                                        val pesoResult = PatientRepository.obtenerHistorialPeso(familyGroupId, patient.id)
                                        val historialPeso = if (pesoResult is Result.Success) pesoResult.data else emptyList()

                                        val bitacoraList = BitacoraRepository.obtenerEntradas(familyGroupId).first()

                                        if (selectedFormato == FormatoExportacion.PDF) {
                                            val pdfUri = ExportHelper.generarPdfHistorial(context, patient, alergias, bitacoraList)
                                            if (pdfUri != null) {
                                                ExportHelper.compartirArchivo(context, pdfUri, "application/pdf", "Compartir Historial PDF")
                                            } else {
                                                Toast.makeText(context, "Error al generar el PDF.", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val csvUri = ExportHelper.generarCsvHistorial(context, patient, historialPeso, bitacoraList)
                                            if (csvUri != null) {
                                                ExportHelper.compartirArchivo(context, csvUri, "text/csv", "Compartir Datos CSV/Excel")
                                            } else {
                                                Toast.makeText(context, "Error al generar el archivo CSV.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Ocurrió un error al exportar.", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGenerando = false
                                    }
                                }
                            },
                            enabled = !isGenerando,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(selectedBrush, RoundedCornerShape(14.dp))
                        ) {
                            if (isGenerando) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Generar y Compartir", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormatoSeleccionableCard(
    titulo: String,
    subtitulo: String,
    descripcion: String,
    chips: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedBrush: Brush
) {
    val borderColor = if (isSelected) Color(0xFF7C4DFF) else Color.LightGray.copy(alpha = 0.5f)
    val bgColor = if (isSelected) Color(0xFFF3E8FF).copy(alpha = 0.5f) else Color.White

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(if (isSelected) Color(0xFF7C4DFF) else Color(0xFFF5F5F5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (isSelected) Color(0xFF7C4DFF) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitulo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF666666)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .then(
                            if (isSelected) Modifier.background(selectedBrush, CircleShape)
                            else Modifier.border(2.dp, Color.LightGray, CircleShape)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = descripcion,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C3E50),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chips.forEach { chipText ->
                    Surface(
                        color = if (isSelected) Color(0xFFEDE7F6) else Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF673AB7) else Color.DarkGray,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

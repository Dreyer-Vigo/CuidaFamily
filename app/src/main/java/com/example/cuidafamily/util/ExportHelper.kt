package com.example.cuidafamily.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.cuidafamily.model.Allergy
import com.example.cuidafamily.model.BitacoraEntrada
import com.example.cuidafamily.model.Patient
import com.example.cuidafamily.model.WeightRecord
import com.example.cuidafamily.model.calcularEdad
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Nota: El PDF es un resumen estructurado generado nativamente con android.graphics.pdf.PdfDocument; no incluye firma digital real ni membrete certificado.
// Nota: El Excel se exporta como archivo CSV compatible con MS Excel y Google Sheets sin requerir librerías pesadas.

object ExportHelper {

    /**
     * Genera un archivo PDF con la ficha médica del paciente, alergias y últimas publicaciones de la bitácora.
     */
    fun generarPdfHistorial(
        context: Context,
        patient: Patient,
        alergias: List<Allergy>,
        bitacora: List<BitacoraEntrada>
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Ancho A4 en puntos (72 dpi)
        val pageHeight = 842 // Alto A4 en puntos

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = AndroidColor.parseColor("#7C4DFF")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = AndroidColor.parseColor("#2C3E50")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = AndroidColor.parseColor("#34495E")
            textSize = 11f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val secondaryPaint = Paint().apply {
            color = AndroidColor.parseColor("#7F8C8D")
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = AndroidColor.parseColor("#E0E0E0")
            strokeWidth = 1f
        }

        var y = 40f

        // 1. Encabezado de la Aplicación
        canvas.drawText("CuidaFamily - Informe Clínico de Salud", 40f, y, titlePaint)
        val fechaGen = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generado el: $fechaGen", 380f, y, secondaryPaint)
        y += 20f

        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 25f

        // 2. Ficha Básica del Paciente
        canvas.drawText("1. DATOS DEL PACIENTE", 40f, y, headerPaint)
        y += 20f

        val edadStr = "${patient.calcularEdad()} años"
        canvas.drawText("Nombre: ${patient.nombre}", 40f, y, bodyPaint)
        canvas.drawText("Edad: $edadStr", 300f, y, bodyPaint)
        y += 18f

        canvas.drawText("DNI: ${patient.dni.ifBlank { "N/A" }}", 40f, y, bodyPaint)
        canvas.drawText("Grupo Sanguíneo: ${patient.grupoSanguineo}", 300f, y, bodyPaint)
        y += 18f

        val emerContacto = if (patient.contactoEmergenciaNombre.isNotBlank())
            "${patient.contactoEmergenciaNombre} (${patient.contactoEmergenciaTelefono})"
        else "Sin registrar"
        canvas.drawText("Contacto Emergencia: $emerContacto", 40f, y, bodyPaint)
        y += 18f

        val seguroStr = patient.seguroSalud.ifBlank { "Ninguno" }
        canvas.drawText("Seguro Médico: $seguroStr", 40f, y, bodyPaint)
        y += 25f

        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 25f

        // 3. Alergias e Intolerancias
        canvas.drawText("2. ALERGIAS E INTOLERANCIAS", 40f, y, headerPaint)
        y += 20f

        if (alergias.isEmpty()) {
            canvas.drawText("No hay alergias registradas.", 40f, y, bodyPaint)
            y += 20f
        } else {
            for (alergia in alergias) {
                if (y > pageHeight - 60f) break
                val textoAlergia = "• ${alergia.nombreAlergeno} (${alergia.severidad.name})"
                val desc = if (alergia.descripcionReaccion.isNotBlank()) " - ${alergia.descripcionReaccion}" else ""
                canvas.drawText(textoAlergia + desc, 40f, y, bodyPaint)
                y += 16f
            }
        }

        y += 10f
        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 25f

        // 4. Bitácora de Novedades (Últimas 15 publicaciones)
        canvas.drawText("3. NOVEDADES RECIENTES DE LA BITÁCORA", 40f, y, headerPaint)
        y += 20f

        val ultimasEntradas = bitacora.take(15)
        if (ultimasEntradas.isEmpty()) {
            canvas.drawText("No hay publicaciones registradas en la bitácora.", 40f, y, bodyPaint)
            y += 20f
        } else {
            for (entrada in ultimasEntradas) {
                if (y > pageHeight - 50f) break

                val animoStr = if (entrada.estadoAnimo.emoji.isNotBlank()) "[${entrada.estadoAnimo.etiqueta}] " else ""
                val headerEntrada = "${entrada.fecha} - ${entrada.autorNombre} (${entrada.autorRol.name}): $animoStr"
                canvas.drawText(headerEntrada, 40f, y, secondaryPaint)
                y += 14f

                if (entrada.nota.isNotBlank()) {
                    val notaCorta = if (entrada.nota.length > 80) entrada.nota.take(80) + "..." else entrada.nota
                    canvas.drawText("  \"$notaCorta\"", 40f, y, bodyPaint)
                    y += 16f
                } else {
                    y += 4f
                }
            }
        }

        pdfDocument.finishPage(page)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "CuidaFamily_Historial_$timeStamp.pdf"

        return guardarArchivoMediaStore(
            context = context,
            fileName = fileName,
            mimeType = "application/pdf",
            writeBlock = { outputStream -> pdfDocument.writeTo(outputStream) }
        ).also {
            pdfDocument.close()
        }
    }

    /**
     * Genera un archivo estructurado CSV compatible con Microsoft Excel y hojas de cálculo.
     */
    fun generarCsvHistorial(
        context: Context,
        patient: Patient,
        historialPeso: List<WeightRecord>,
        bitacora: List<BitacoraEntrada>
    ): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "CuidaFamily_Datos_$timeStamp.csv"

        val sb = StringBuilder()

        // 1. Datos Paciente
        sb.append("=== FICHA DEL PACIENTE ===\n")
        sb.append("Nombre,Edad,DNI,Grupo Sanguineo,Seguro\n")
        sb.append("\"${patient.nombre}\",\"${patient.calcularEdad()} años\",\"${patient.dni}\",\"${patient.grupoSanguineo}\",\"${patient.seguroSalud}\"\n\n")

        // 2. Historial de Peso
        sb.append("=== HISTORIAL DE PESO ===\n")
        sb.append("Fecha,Peso_Kg\n")
        if (historialPeso.isEmpty()) {
            sb.append("Sin registros,0.0\n\n")
        } else {
            for (peso in historialPeso) {
                sb.append("\"${peso.fecha}\",${peso.valorKg}\n")
            }
            sb.append("\n")
        }

        // 3. Bitácora de Novedades
        sb.append("=== BITACORA DE NOVEDADES ===\n")
        sb.append("Fecha,Autor,Rol,EstadoAnimo,Nota\n")
        if (bitacora.isEmpty()) {
            sb.append("Sin registros,,,,\n")
        } else {
            for (entrada in bitacora) {
                val notaLimpia = entrada.nota.replace("\"", "\"\"").replace("\n", " ")
                sb.append("\"${entrada.fecha}\",\"${entrada.autorNombre}\",\"${entrada.autorRol.name}\",\"${entrada.estadoAnimo.etiqueta}\",\"$notaLimpia\"\n")
            }
        }

        val csvContent = sb.toString()

        return guardarArchivoMediaStore(
            context = context,
            fileName = fileName,
            mimeType = "text/csv",
            writeBlock = { outputStream -> outputStream.write(csvContent.toByteArray(Charsets.UTF_8)) }
        )
    }

    /**
     * Guarda el archivo en MediaStore (Android 10+) o en el directorio de descargas/caché con FileProvider.
     */
    private fun guardarArchivoMediaStore(
        context: Context,
        fileName: String,
        mimeType: String,
        writeBlock: (FileOutputStream) -> Unit
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        (os as? FileOutputStream)?.let { writeBlock(it) }
                            ?: run {
                                val tempFile = File(context.cacheDir, fileName)
                                FileOutputStream(tempFile).use { writeBlock(it) }
                                tempFile.inputStream().use { input -> os.write(input.readBytes()) }
                            }
                    }
                    Log.d("ExportHelper", "Archivo guardado en MediaStore.Downloads: $uri")
                    uri
                } else null
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, fileName)
                FileOutputStream(targetFile).use { os -> writeBlock(os) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
                Log.d("ExportHelper", "Archivo guardado en descargas: $uri")
                uri
            }
        } catch (e: Exception) {
            Log.e("ExportHelper", "Error guardando archivo $fileName", e)
            try {
                // Fallback a CacheDir + FileProvider
                val cacheFile = File(context.cacheDir, fileName)
                FileOutputStream(cacheFile).use { os -> writeBlock(os) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                Log.d("ExportHelper", "Archivo guardado en CacheDir (fallback): $uri")
                uri
            } catch (ex: Exception) {
                Log.e("ExportHelper", "Fallback de archivo también falló", ex)
                null
            }
        }
    }

    /**
     * Abre el selector de compartir de Android para enviar el archivo mediante WhatsApp, Correo, etc.
     */
    fun compartirArchivo(context: Context, fileUri: Uri, mimeType: String, titulo: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, titulo).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("ExportHelper", "Error al compartir archivo", e)
        }
    }
}

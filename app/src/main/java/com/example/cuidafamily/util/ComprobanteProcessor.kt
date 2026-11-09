package com.example.cuidafamily.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ComprobanteProcessResult(
    val base64List: List<String> = emptyList(),
    val origBytes: Long = 0L,
    val totalPages: Int = 0,
    val processedPages: Int = 0,
    val totalBase64Chars: Int = 0,
    val mimeType: String = "",
    val errorLectura: Boolean = false,
    val errorDemasiadoGrande: Boolean = false
)

object ComprobanteProcessor {

    suspend fun procesarComprobanteUri(
        context: Context,
        uri: Uri,
        origenStr: String = "archivo"
    ): ComprobanteProcessResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Ignore if takePersistableUriPermission is not supported for this URI
        }

        val mimeType = try {
            contentResolver.getType(uri) ?: ""
        } catch (e: Exception) {
            Log.e("Gastos", "Fallo en comprobante al obtener mimeType: ${e.message}", e)
            ""
        }

        val origBytes = try {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (e: Exception) {
            0L
        }

        val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.toString().endsWith(".pdf", ignoreCase = true)

        if (isPdf) {
            procesarPdf(context, uri, origenStr, if (mimeType.isBlank()) "application/pdf" else mimeType, origBytes)
        } else {
            procesarImagen(context, uri, origenStr, if (mimeType.isBlank()) "image/jpeg" else mimeType, origBytes)
        }
    }

    private fun procesarImagen(
        context: Context,
        uri: Uri,
        origen: String,
        mime: String,
        bytesOrig: Long
    ): ComprobanteProcessResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("Gastos", "Fallo en comprobante: InputStream nulo para $uri")
                return ComprobanteProcessResult(errorLectura = true, mimeType = mime, origBytes = bytesOrig)
            }

            var originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                Log.e("Gastos", "Fallo en comprobante: No se pudo decodificar la imagen $uri")
                return ComprobanteProcessResult(errorLectura = true, mimeType = mime, origBytes = bytesOrig)
            }

            originalBitmap = rotateBitmapIfRequired(context, uri, originalBitmap)

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

                while (outputStream.toByteArray().size > 450 * 1024 && quality > 40) {
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
                val c = finalBase64.length
                Log.d("Gastos", "Comprobante: origen=$origen, mime=$mime, bytesOriginal=$bytesOrig, paginas=1, caracteresBase64=$c")
                ComprobanteProcessResult(
                    base64List = listOf(finalBase64),
                    origBytes = bytesOrig,
                    totalPages = 1,
                    processedPages = 1,
                    totalBase64Chars = c,
                    mimeType = mime
                )
            } else {
                Log.e("Gastos", "Fallo en comprobante: Excede límite de caracteres Base64")
                ComprobanteProcessResult(errorDemasiadoGrande = true, origBytes = bytesOrig, mimeType = mime)
            }
        } catch (e: Exception) {
            Log.e("Gastos", "Fallo en comprobante: ${e.message}", e)
            ComprobanteProcessResult(errorLectura = true, origBytes = bytesOrig, mimeType = mime)
        }
    }

    private fun procesarPdf(
        context: Context,
        uri: Uri,
        origen: String,
        mime: String,
        bytesOrig: Long
    ): ComprobanteProcessResult {
        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null
        return try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor == null) {
                Log.e("Gastos", "Fallo en comprobante: ParcelFileDescriptor nulo para PDF $uri")
                return ComprobanteProcessResult(errorLectura = true, origBytes = bytesOrig, mimeType = mime)
            }

            pdfRenderer = PdfRenderer(fileDescriptor)
            val totalPages = pdfRenderer.pageCount
            val pagesToProcess = minOf(totalPages, 3)
            val resultList = mutableListOf<String>()
            var totalChars = 0

            for (i in 0 until pagesToProcess) {
                val page = pdfRenderer.openPage(i)
                val pageWidth = 1000
                val pageHeight = (1000f * page.height / page.width).toInt()

                val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                var quality = 80
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

                while (outputStream.toByteArray().size > 300 * 1024 && quality > 40) {
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

            if (resultList.isNotEmpty()) {
                Log.d("Gastos", "Comprobante: origen=$origen, mime=$mime, bytesOriginal=$bytesOrig, paginas=${resultList.size}, caracteresBase64=$totalChars")
                ComprobanteProcessResult(
                    base64List = resultList,
                    origBytes = bytesOrig,
                    totalPages = totalPages,
                    processedPages = resultList.size,
                    totalBase64Chars = totalChars,
                    mimeType = mime
                )
            } else {
                Log.e("Gastos", "Fallo en comprobante PDF: Excede límite de tamaño Base64")
                ComprobanteProcessResult(errorDemasiadoGrande = true, origBytes = bytesOrig, mimeType = mime)
            }
        } catch (e: Exception) {
            Log.e("Gastos", "Fallo en comprobante: ${e.message}", e)
            ComprobanteProcessResult(errorLectura = true, origBytes = bytesOrig, mimeType = mime)
        } finally {
            try { pdfRenderer?.close() } catch (e: Exception) {}
            try { fileDescriptor?.close() } catch (e: Exception) {}
        }
    }

    private fun rotateBitmapIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exifInterface = ExifInterface(inputStream)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("Gastos", "Fallo en comprobante al decodificar Base64: ${e.message}", e)
            null
        }
    }
}

package com.example.cuidafamily.model

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Niveles de severidad aplicables a alergias o condiciones del paciente.
 */
enum class Severidad {
    LEVE,
    MODERADA,
    SEVERA
}

/**
 * Representa un registro histórico del peso del paciente.
 */
data class WeightRecord(
    val fecha: String = "",
    val valorKg: Double = 0.0
)

/**
 * Alergia o intolerancia diagnosticada en el paciente.
 */
data class Allergy(
    val id: String = "",
    val nombreAlergeno: String = "",
    val severidad: Severidad = Severidad.LEVE,
    val descripcionReaccion: String = ""
)

/**
 * Representa al ser querido o persona dependiente que recibe los cuidados médicos y asistencia familiar.
 */
data class Patient(
    val id: String = "",
    val nombre: String = "",
    val fechaNacimiento: String = "",
    val dni: String = "",
    val fotoUrl: String? = null,
    val grupoSanguineo: String = "O+",
    val pesoActual: Double = 0.0,
    
    // Datos Clínicos
    val historiaClinica: String = "",
    val seguroSalud: String = "",
    val medicoTratante: String = "",
    
    // Contacto de Emergencia
    val contactoEmergenciaNombre: String = "",
    val contactoEmergenciaTelefono: String = "",
    val contactoEmergenciaRelacion: String = "",
    
    // Secciones de texto libre
    val enfermedadesCronicas: String = "",
    val medicamentosActuales: String = "",
    val informacionAdicional: String? = null,
    
    val familyGroupId: String = ""
)

/**
 * Función de extensión para calcular la edad exacta del paciente en años.
 */
fun Patient.calcularEdad(): Int {
    if (this.fechaNacimiento.isBlank()) return 0
    
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val fechaNac: Date = sdf.parse(this.fechaNacimiento) ?: return 0
        
        val calNac = Calendar.getInstance().apply { time = fechaNac }
        val calHoy = Calendar.getInstance()

        var edad = calHoy.get(Calendar.YEAR) - calNac.get(Calendar.YEAR)

        if (calHoy.get(Calendar.MONTH) < calNac.get(Calendar.MONTH) || 
            (calHoy.get(Calendar.MONTH) == calNac.get(Calendar.MONTH) && 
             calHoy.get(Calendar.DAY_OF_MONTH) < calNac.get(Calendar.DAY_OF_MONTH))) {
            edad--
        }
        
        if (edad < 0) 0 else edad
    } catch (e: Exception) {
        Log.e("PatientModel", "Error al calcular edad para fecha: ${this.fechaNacimiento}", e)
        0
    }
}

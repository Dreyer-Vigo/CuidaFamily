package com.example.cuidafamily.model

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class Severidad {
    LEVE,
    MODERADA,
    SEVERA
}

data class WeightRecord(
    val fecha: String = "",
    val valorKg: Double = 0.0
)

data class Allergy(
    val id: String = "",
    val nombreAlergeno: String = "",
    val severidad: Severidad = Severidad.LEVE,
    val descripcionReaccion: String = ""
)

data class Patient(
    val id: String = "",
    val nombre: String = "",
    val fechaNacimiento: String = "",
    val dni: String = "",
    val fotoUrl: String? = null,
    val grupoSanguineo: String = "O+",
    val pesoActual: Double = 0.0,
    val historiaClinica: String = "",
    val seguroSalud: String = "",
    val medicoTratante: String = "",
    val contactoEmergenciaNombre: String = "",
    val contactoEmergenciaTelefono: String = "",
    val contactoEmergenciaRelacion: String = "",
    val enfermedadesCronicas: String = "",
    val medicamentosActuales: String = "",
    val informacionAdicional: String? = null,
    val familyGroupId: String = ""
)

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

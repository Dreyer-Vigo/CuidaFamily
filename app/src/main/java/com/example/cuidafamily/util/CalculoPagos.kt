package com.example.cuidafamily.util

import com.example.cuidafamily.model.ExcepcionTurno
import com.example.cuidafamily.model.Pago
import com.example.cuidafamily.model.SolicitudIntercambio
import com.example.cuidafamily.model.Turno
import com.example.cuidafamily.model.TurnoCheckIn
import com.example.cuidafamily.model.cuidadorEnFecha
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Detalle de una ocurrencia (turno + fecha) en el cálculo financiero.
 */
data class OcurrenciaDetalle(
    val turnoId: String,
    val fechaIso: String, // "yyyy-MM-dd"
    val fechaLocalDate: LocalDate,
    val horaInicio: String,
    val horaFin: String,
    val horas: Double,
    val tarifaPorHora: Double?, // null o <=0 si "Sin tarifa"
    val montoDevengado: Double,
    val estadoCodigo: String, // "CUMPLIDA", "SIN_CHECKIN", "NO_CUMPLIDA"
    val estadoEtiqueta: String, // "✅ Cumplida", "⚠️ Sin check-in", "❌ No cumplida"
    val esReemplazo: Boolean,
    val esCumplida: Boolean,
    val cuidadorUserId: String,
    val cuidadorNombre: String,
    val checkInExistente: TurnoCheckIn?
)

/**
 * Resumen de cálculo financiero para un cuidador en un mes específico.
 */
data class ResumenPagosCuidador(
    val cuidadorUserId: String,
    val cuidadorNombre: String,
    val subtipo: String, // Enfermero/a, Cuidador/a
    val turnosCumplidosCount: Int,
    val totalHoras: Double,
    val devengado: Double,
    val pagado: Double,
    val pendiente: Double, // devengado - pagado (si < 0, representa un adelanto)
    val tieneTurnoSinTarifa: Boolean,
    val ocurrencias: List<OcurrenciaDetalle>
)

object CalculoPagos {

    /**
     * Calcula la cantidad exacta de horas laboradas en un turno.
     * Regla:
     * - Si horaFin < horaInicio, el turno cruza la medianoche (suma 24h).
     * - Si horaFin == horaInicio, equivale a un turno continuo de 24h.
     */
    fun calcularHorasTurno(horaInicioStr: String, horaFinStr: String): Double {
        return try {
            val partesIni = horaInicioStr.split(":")
            val partesFin = horaFinStr.split(":")
            val hIni = partesIni[0].toInt()
            val mIni = partesIni[1].toInt()
            val hFin = partesFin[0].toInt()
            val mFin = partesFin[1].toInt()

            val minIni = hIni * 60 + mIni
            val minFin = hFin * 60 + mFin

            val diffMin = when {
                minFin < minIni -> (minFin + 24 * 60) - minIni
                minFin == minIni -> 24 * 60
                else -> minFin - minIni
            }

            BigDecimal.valueOf(diffMin.toLong())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Verifica si una ocurrencia (turno + fecha) ya ha terminado en el tiempo actual.
     */
    fun esOcurrenciaTerminada(date: LocalDate, horaFinStr: String): Boolean {
        val hoy = LocalDate.now()
        if (date.isBefore(hoy)) return true
        if (date.isAfter(hoy)) return false
        return try {
            val ahora = LocalTime.now()
            val partesFin = horaFinStr.split(":")
            val horaFinTime = LocalTime.of(partesFin[0].toInt(), partesFin[1].toInt())
            ahora.isAfter(horaFinTime)
        } catch (e: Exception) {
            true
        }
    }

    /**
     * CÁLCULO PRINCIPAL PURA:
     * Para un cuidador y un mes objetivo (targetYearMonth):
     * 1. TURNO CUMPLIDO: una ocurrencia (turno + fecha) dentro del periodo, ya terminada,
     *    atribuida a cuidadorEnFecha(turno, fecha) (considerando intercambios aprobados).
     * 2. TARIFA: Si la tarifa cambia, el cálculo usa la vigente actualmente en el mapa de tarifas.
     * 3. devengado = Σ horas × tarifaPorHora
     * 4. pagado = Σ pagos del cuidador imputados al mes de periodoFin
     * 5. pendiente = devengado − pagado. Si es negativo, se presenta como "Adelanto de S/ X".
     */
    fun calcularResumenCuidadorMes(
        cuidadorUserId: String,
        cuidadorNombre: String,
        subtipo: String,
        targetYearMonth: YearMonth,
        turnos: List<Turno>,
        tarifasMap: Map<String, Double>, // turnoId -> tarifaPorHora vigente
        checkIns: List<TurnoCheckIn>,
        excepciones: List<ExcepcionTurno>,
        solicitudes: List<SolicitudIntercambio>,
        pagos: List<Pago>,
        contarSinCheckIn: Boolean // Interruptor SharedPreferences
    ): ResumenPagosCuidador {
        val maxDiasMes = targetYearMonth.lengthOfMonth()
        val ocurrencias = mutableListOf<OcurrenciaDetalle>()

        var totalHorasBd = BigDecimal.ZERO
        var totalDevengadoBd = BigDecimal.ZERO
        var turnosCumplidosCount = 0
        var tieneTurnoSinTarifa = false

        for (diaNum in 1..maxDiasMes) {
            val fechaDate = targetYearMonth.atDay(diaNum)
            val fechaIso = fechaDate.toString()

            // Buscar todos los turnos que aplican a esta fecha
            for (turno in turnos) {
                if (!turno.activo) continue
                if (!turno.aplicaEnFecha(fechaDate)) continue

                // Determinar quién es el cuidador responsable en esta fecha específica (intercambios aprobados)
                val asignacion = cuidadorEnFecha(turno, fechaDate, excepciones, solicitudes)
                if (asignacion.userId != cuidadorUserId) continue

                // TURNO CUMPLIDO: debe estar dentro del periodo y estar ya terminado
                if (!esOcurrenciaTerminada(fechaDate, turno.horaFin)) continue

                val checkIn = checkIns.firstOrNull { it.turnoId == turno.id && it.fecha == fechaIso }

                // Evaluamos los estados del TurnoCheckIn:
                // - noCumplido == true -> Marca explícita de No Cumplida
                // - manual == true || ubicacionValida == true -> Marca explícita de Cumplida o GPS válido
                // - Sin check-in -> depende de contarSinCheckIn
                val (esCumplida, estadoCod, estadoEtiq) = when {
                    checkIn?.noCumplido == true -> Triple(false, "NO_CUMPLIDA", "❌ No cumplida")
                    checkIn?.manual == true -> Triple(true, "CUMPLIDA", "✅ Cumplida")
                    checkIn?.ubicacionValida == true -> Triple(true, "CUMPLIDA", "✅ Cumplida")
                    else -> {
                        if (contarSinCheckIn) {
                            Triple(true, "SIN_CHECKIN", "⚠️ Sin check-in")
                        } else {
                            Triple(false, "SIN_CHECKIN", "⚠️ Sin check-in")
                        }
                    }
                }

                val horasTurno = calcularHorasTurno(turno.horaInicio, turno.horaFin)
                val horasBd = BigDecimal.valueOf(horasTurno).setScale(2, RoundingMode.HALF_UP)

                // Si la tarifa cambia, el cálculo usa la vigente actualmente guardada en familyGroups/{groupId}/tarifas/{turnoId}.
                val tarifaVigente = tarifasMap[turno.id]
                val tieneTarifaValida = tarifaVigente != null && tarifaVigente > 0.0

                val montoDevengadoOcurrenciaBd = if (esCumplida && tieneTarifaValida) {
                    val tarifaBd = BigDecimal.valueOf(tarifaVigente!!).setScale(2, RoundingMode.HALF_UP)
                    horasBd.multiply(tarifaBd).setScale(2, RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }

                if (!tieneTarifaValida && esCumplida) {
                    tieneTurnoSinTarifa = true
                }

                if (esCumplida) {
                    turnosCumplidosCount++
                    totalHorasBd = totalHorasBd.add(horasBd)
                    totalDevengadoBd = totalDevengadoBd.add(montoDevengadoOcurrenciaBd)
                }

                ocurrencias.add(
                    OcurrenciaDetalle(
                        turnoId = turno.id,
                        fechaIso = fechaIso,
                        fechaLocalDate = fechaDate,
                        horaInicio = turno.horaInicio,
                        horaFin = turno.horaFin,
                        horas = horasTurno,
                        tarifaPorHora = if (tieneTarifaValida) tarifaVigente else null,
                        montoDevengado = montoDevengadoOcurrenciaBd.toDouble(),
                        estadoCodigo = estadoCod,
                        estadoEtiqueta = estadoEtiq,
                        esReemplazo = asignacion.esReemplazo,
                        esCumplida = esCumplida,
                        cuidadorUserId = asignacion.userId,
                        cuidadorNombre = asignacion.nombre,
                        checkInExistente = checkIn
                    )
                )
            }
        }

        // CÁLCULO DE PAGADO: Σ pagos del cuidador imputados al mes de periodoFin
        var totalPagadoBd = BigDecimal.ZERO
        val pagosDelCuidador = pagos.filter { it.cuidadorUserId == cuidadorUserId }

        val targetMonthStr = targetYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        for (pago in pagosDelCuidador) {
            val fechaFinImputada = pago.periodoFin.ifBlank { pago.fecha }
            if (fechaFinImputada.startsWith(targetMonthStr)) {
                val montoPagoBd = BigDecimal.valueOf(pago.monto).setScale(2, RoundingMode.HALF_UP)
                totalPagadoBd = totalPagadoBd.add(montoPagoBd)
            }
        }

        val pendienteBd = totalDevengadoBd.subtract(totalPagadoBd).setScale(2, RoundingMode.HALF_UP)

        return ResumenPagosCuidador(
            cuidadorUserId = cuidadorUserId,
            cuidadorNombre = cuidadorNombre,
            subtipo = subtipo,
            turnosCumplidosCount = turnosCumplidosCount,
            totalHoras = totalHorasBd.toDouble(),
            devengado = totalDevengadoBd.toDouble(),
            pagado = totalPagadoBd.toDouble(),
            pendiente = pendienteBd.toDouble(),
            tieneTurnoSinTarifa = tieneTurnoSinTarifa,
            ocurrencias = ocurrencias.sortedBy { it.fechaIso }
        )
    }
}

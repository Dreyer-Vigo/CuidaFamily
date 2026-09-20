package com.example.cuidafamily.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cuidafamily.ui.theme.LavandaPrimary
import java.time.LocalTime
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WheelTimePicker(
    horaInicial: LocalTime,
    onTimeChanged: (LocalTime) -> Unit
) {
    // Conversión a formato 12h para la visualización en ruedas
    val isInitialPm = horaInicial.hour >= 12
    val initialHour12 = when {
        horaInicial.hour == 0 -> 12
        horaInicial.hour > 12 -> horaInicial.hour - 12
        else -> horaInicial.hour
    }

    val hours = (1..12).toList()
    val minutes = (0..59).toList()
    val amPm = listOf("AM", "PM")

    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = initialHour12 - 1)
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = horaInicial.minute)
    val amPmState = rememberLazyListState(initialFirstVisibleItemIndex = if (isInitialPm) 1 else 0)

    // Sincronización de vuelta a LocalTime cuando cambian las ruedas
    LaunchedEffect(hourState, minuteState, amPmState) {
        snapshotFlow {
            Triple(hourState.firstVisibleItemIndex, minuteState.firstVisibleItemIndex, amPmState.firstVisibleItemIndex)
        }
        .distinctUntilChanged()
        .collect { (hIdx, mIdx, apIdx) ->
            val h12 = hours.getOrElse(hIdx) { 1 }
            val m = minutes.getOrElse(mIdx) { 0 }
            val isPm = apIdx == 1
            
            val h24 = when {
                !isPm && h12 == 12 -> 0
                !isPm -> h12
                isPm && h12 == 12 -> 12
                else -> h12 + 12
            }
            onTimeChanged(LocalTime.of(h24, m))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Marcador visual de zona de selección (líneas horizontales)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalDivider(color = LavandaPrimary.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(44.dp))
            HorizontalDivider(color = LavandaPrimary.copy(alpha = 0.3f), thickness = 1.dp)
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelColumn(items = hours, state = hourState, label = { it.toString() })
            Text(
                text = ":", 
                fontSize = 22.sp, 
                fontWeight = FontWeight.Bold, 
                color = LavandaPrimary, 
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            WheelColumn(items = minutes, state = minuteState, label = { it.toString().padStart(2, '0') })
            Spacer(modifier = Modifier.width(16.dp))
            WheelColumn(items = amPm, state = amPmState, label = { it })
        }
    }
}

@Composable
private fun <T> WheelColumn(
    items: List<T>,
    state: androidx.compose.foundation.lazy.LazyListState,
    label: (T) -> String
) {
    val itemHeight = 44.dp
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)

    LazyColumn(
        state = state,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight), // Padding para permitir centrar primer/último ítem
        modifier = Modifier
            .width(60.dp)
            .height(itemHeight * 3),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items.size) { index ->
            val item = items[index]
            
            // Cálculo de opacidad y escala basado en la distancia al centro del viewport
            val distanceFactor = remember {
                derivedStateOf {
                    val layoutInfo = state.layoutInfo
                    val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == index }
                    if (visibleItem != null) {
                        val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                        val itemCenter = visibleItem.offset + visibleItem.size / 2f
                        val dist = Math.abs(center - itemCenter)
                        (dist / layoutInfo.viewportEndOffset).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                }
            }

            val scale by remember { derivedStateOf { (1f - (distanceFactor.value * 1.5f)).coerceIn(0.7f, 1.1f) } }
            val alpha by remember { derivedStateOf { (1f - (distanceFactor.value * 2.5f)).coerceIn(0.3f, 1f) } }
            val isSelected = distanceFactor.value < 0.15f

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(item),
                    fontSize = if (isSelected) 22.sp else 18.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isSelected) LavandaPrimary else Color.Gray
                )
            }
        }
    }
}

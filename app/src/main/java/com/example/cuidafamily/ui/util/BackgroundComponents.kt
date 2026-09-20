package com.example.cuidafamily.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cuidafamily.ui.theme.BgEnd
import com.example.cuidafamily.ui.theme.BgStart
import com.example.cuidafamily.ui.theme.BlobPink
import com.example.cuidafamily.ui.theme.BlobTurquoise
import com.example.cuidafamily.ui.theme.GradientStarEnd
import com.example.cuidafamily.ui.theme.GradientStarStart

/**
 * Fondo artístico premium con gradiente diagonal y blobs vibrantes difuminados.
 */
@Composable
fun AppBackgroundDecorated(
    content: @Composable () -> Unit
) {
    val bgBrush = Brush.linearGradient(
        colors = listOf(BgStart, BgEnd),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Blob rosa superior derecho
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BlobPink.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width * 0.8f, 200f),
                    radius = 600f
                ),
                radius = 600f,
                center = Offset(size.width * 0.8f, 200f)
            )
            // Blob turquesa inferior izquierdo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BlobTurquoise.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(200f, size.height * 0.8f),
                    radius = 700f
                ),
                radius = 700f,
                center = Offset(200f, size.height * 0.8f)
            )
        }
        content()
    }
}

/**
 * Botón premium con gradiente "estrella" (Rosa-Violeta) y animación de pulsación.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "buttonScale"
    )

    val startColor = if (enabled) GradientStarStart else Color.LightGray.copy(alpha = 0.5f)
    val endColor = if (enabled) GradientStarEnd else Color.LightGray.copy(alpha = 0.5f)
    
    val gradient = Brush.horizontalGradient(listOf(startColor, endColor))

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = GradientStarStart,
                ambientColor = GradientStarStart.copy(alpha = 0.4f)
            )
            .height(54.dp)
            .background(gradient, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/**
 * Avatar circular premium con la inicial del usuario y efecto de glow.
 */
@Composable
fun UserAvatar(
    nombre: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp
) {
    val inicial = nombre.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 8.dp, 
                shape = CircleShape, 
                spotColor = com.example.cuidafamily.ui.theme.GradientStarStart
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        com.example.cuidafamily.ui.theme.GradientStarStart, 
                        com.example.cuidafamily.ui.theme.GradientStarEnd
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = inicial,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.45).sp
        )
    }
}

package com.xfyc.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DefaultCover(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                    start = Offset.Zero,
                    end = Offset(size.value, size.value)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = size.toPx() * 0.018f
            val baseY = this.size.height * 0.7f
            repeat(5) { index ->
                val x = this.size.width * (0.18f + index * 0.16f)
                drawLine(
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                    start = Offset(x, baseY),
                    end = Offset(x, this.size.height * (0.38f + index % 3 * 0.08f)),
                    strokeWidth = stroke
                )
            }
        }
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(size * 0.6f),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
        )
    }
}

package com.xfyc.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PlayerAccent,
    onPrimary = Color(0xFF032016),
    primaryContainer = Color(0xFF0B3B2A),
    onPrimaryContainer = PlayerAccentSoft,
    secondary = FogGrey80,
    tertiary = PlayerWarm,
    surface = PlayerSurface,
    surfaceVariant = PlayerSurfaceHigh,
    background = PlayerBackground,
    onBackground = PlayerOnSurface,
    onSurface = PlayerOnSurface,
    onSurfaceVariant = Color(0xFFB8C7C0),
    outline = Color(0xFF44524D)
)

private val LightColorScheme = lightColorScheme(
    primary = Pine40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F6E6),
    onPrimaryContainer = Color(0xFF063322),
    secondary = Slate40,
    tertiary = Coral40,
    background = Color(0xFFF8FAF7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EFEA),
    onSurface = Color(0xFF121816),
    onSurfaceVariant = Color(0xFF5A6862),
    outline = Color(0xFFD6DED9)
)

@Composable
fun XfycMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        ),
        typography = Typography,
        content = content
    )
}

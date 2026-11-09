package com.example.filtro400.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val FormasCentinela = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val EsquemaClaro = lightColorScheme(
    primary = AzulNoche,
    onPrimary = Color.White,
    primaryContainer = AzulNocheMuyClaro,
    onPrimaryContainer = AzulNoche,
    inversePrimary = Color(0xFFB8D2FA),
    secondary = VerdeMentaOscuro,
    onSecondary = Color.White,
    secondaryContainer = VerdeMentaClaro,
    onSecondaryContainer = AzulNoche,
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = CoralClaro,
    onTertiaryContainer = CoralOscuro,
    background = FondoClaro,
    onBackground = TextoClaro,
    surface = Color.White,
    onSurface = TextoClaro,
    surfaceVariant = Color(0xFFE9EEF5),
    onSurfaceVariant = GrisSuave,
    error = Coral,
    onError = Color.White,
    errorContainer = CoralClaro,
    onErrorContainer = CoralOscuro,
    outline = Color(0xFFC3CCD8)
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFFB8D2FA),
    onPrimary = AzulNocheProfundo,
    primaryContainer = Color(0xFF243D5D),
    onPrimaryContainer = Color(0xFFE0EBFA),
    inversePrimary = AzulNoche,
    secondary = VerdeMenta,
    onSecondary = VerdeMentaProfundo,
    secondaryContainer = Color(0xFF124B40),
    onSecondaryContainer = Color(0xFFB9F4DF),
    tertiary = Coral,
    onTertiary = AzulNoche,
    tertiaryContainer = Color(0xFF5A2529),
    onTertiaryContainer = Color(0xFFFFD9DA),
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = SuperficieOscura,
    onSurface = TextoOscuro,
    surfaceDim = FondoOscuro,
    surfaceBright = Color(0xFF304761),
    surfaceContainerLowest = Color(0xFF06101F),
    surfaceContainerLow = Color(0xFF0B1A2D),
    surfaceContainer = SuperficieOscura,
    surfaceContainerHigh = SuperficieElevadaOscura,
    surfaceContainerHighest = Color(0xFF203753),
    surfaceVariant = Color(0xFF1B304B),
    onSurfaceVariant = GrisOscuro,
    error = Color(0xFFFF8A8D),
    onError = AzulNoche,
    errorContainer = Color(0xFF5A1E21),
    onErrorContainer = CoralClaro,
    outline = Color(0xFF3A506B)
)

@Composable
fun CentinelaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EsquemaOscuro
        else -> EsquemaClaro
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FormasCentinela,
        content = content
    )
}

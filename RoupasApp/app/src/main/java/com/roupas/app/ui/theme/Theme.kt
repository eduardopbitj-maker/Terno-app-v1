package com.roupas.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta "alfaiataria" — tons escuros, elegantes, com um dourado sutil de destaque.
val FundoEscuro = Color(0xFF10120F)
val SuperficieEscura = Color(0xFF1B1D19)
val SuperficieElevada = Color(0xFF24261F)
val DouradoDestaque = Color(0xFFC9A24B)
val VermelhoUso = Color(0xFFE0473C)
val AmareloUso = Color(0xFFE0C23C)
val VerdeUso = Color(0xFF3CAA5C)
val TextoPrincipal = Color(0xFFF3F1EC)
val TextoSecundario = Color(0xFFB5B2A9)

private val EsquemaEscuro = darkColorScheme(
    primary = DouradoDestaque,
    onPrimary = Color(0xFF241A02),
    background = FundoEscuro,
    onBackground = TextoPrincipal,
    surface = SuperficieEscura,
    onSurface = TextoPrincipal,
    surfaceVariant = SuperficieElevada,
    onSurfaceVariant = TextoSecundario,
    error = VermelhoUso
)

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF8A6D22),
    background = Color(0xFFFAF8F3),
    surface = Color(0xFFFFFFFF)
)

val TituloGrande = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
val TituloMedio = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
val Corpo = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
val Legenda = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)

@Composable
fun RoupasAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val esquema = if (darkTheme) EsquemaEscuro else EsquemaClaro
    MaterialTheme(colorScheme = esquema, content = content)
}

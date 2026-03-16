package com.pokemonshowdown.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette Pokémon Showdown
val PokeRed = Color(0xFFCC0000)
val PokeRedDark = Color(0xFF990000)
val PokeBlue = Color(0xFF003A8C)
val PokeBlueDark = Color(0xFF002060)
val PokeYellow = Color(0xFFFFCB05)
val PokeGray = Color(0xFF1A1A2E)
val PokeGrayLight = Color(0xFF16213E)
val PokeSurface = Color(0xFF0F3460)
val PokeOnSurface = Color(0xFFE0E0E0)
val PokeGreen = Color(0xFF4CAF50)
val PokeOrange = Color(0xFFFF9800)

// HP Colors
val HpGreen = Color(0xFF5DB85D)
val HpYellow = Color(0xFFD4C000)
val HpRed = Color(0xFFCC0000)

private val DarkColorScheme = darkColorScheme(
    primary = PokeRed,
    onPrimary = Color.White,
    primaryContainer = PokeRedDark,
    secondary = PokeYellow,
    onSecondary = Color.Black,
    background = PokeGray,
    onBackground = Color.White,
    surface = PokeGrayLight,
    onSurface = PokeOnSurface,
    surfaceVariant = PokeSurface,
    error = Color(0xFFCF6679)
)

@Composable
fun PokemonShowdownTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}

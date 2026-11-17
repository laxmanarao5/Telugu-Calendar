package com.telugucalendar.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.telugucalendar.app.data.ThemeMode

// Kumkum red, turmeric gold and a cream paper background, like a printed Telugu wall calendar.
private val LightColors = lightColorScheme(
    primary = Color(0xFFB3261E), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD4), onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF8A5100), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB3), onSecondaryContainer = Color(0xFF2C1600),
    tertiary = Color(0xFF2E7D32), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8E6C9), onTertiaryContainer = Color(0xFF00210A),
    background = Color(0xFFFFFBF2), onBackground = Color(0xFF211A14),
    surface = Color(0xFFFFFBF2), onSurface = Color(0xFF211A14),
    surfaceVariant = Color(0xFFF5E6D3), onSurfaceVariant = Color(0xFF514438),
    outline = Color(0xFF857467), outlineVariant = Color(0xFFD8C3B0),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFF8EA),
    surfaceContainer = Color(0xFFFFF3DF), surfaceContainerHigh = Color(0xFFFBEBD3),
    surfaceContainerHighest = Color(0xFFF5E4CB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A9), onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A), onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFFFB95C), onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3C00), onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = Color(0xFF81C995), onTertiary = Color(0xFF00390F),
    tertiaryContainer = Color(0xFF1B5E20), onTertiaryContainer = Color(0xFFC8E6C9),
    background = Color(0xFF17120E), onBackground = Color(0xFFEDE0D6),
    surface = Color(0xFF17120E), onSurface = Color(0xFFEDE0D6),
    surfaceVariant = Color(0xFF514438), onSurfaceVariant = Color(0xFFD8C3B0),
    outline = Color(0xFF9F8D80), outlineVariant = Color(0xFF514438),
    surfaceContainerLowest = Color(0xFF120D09), surfaceContainerLow = Color(0xFF1F1A15),
    surfaceContainer = Color(0xFF241E19), surfaceContainerHigh = Color(0xFF2F2823),
    surfaceContainerHighest = Color(0xFF3A332D),
)

/** Colours the wall calendar needs beyond Material's scheme. */
@Immutable
class CalendarColors(
    val holidayRed: Color,
    val headerStart: Color,
    val headerEnd: Color,
    val onHeader: Color,
    val vratam: Color,
)

val LocalCalendarColors = staticCompositionLocalOf {
    CalendarColors(Color(0xFFC62828), Color(0xFFB3261E), Color(0xFFE08A00), Color.White, Color(0xFFB26A00))
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TeluguCalendarTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val colors = if (dark) {
        CalendarColors(Color(0xFFFF8A80), Color(0xFF7A1A14), Color(0xFF8A5100), Color(0xFFFFF3E0), Color(0xFFFFB95C))
    } else {
        CalendarColors(Color(0xFFC62828), Color(0xFFB3261E), Color(0xFFE08A00), Color.White, Color(0xFFB26A00))
    }
    CompositionLocalProvider(LocalCalendarColors provides colors) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, shapes = AppShapes, content = content)
    }
}

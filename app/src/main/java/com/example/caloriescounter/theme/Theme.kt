package com.example.caloriescounter.theme

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

enum class ThemePalette {
    CYAN, EMERALD, TEAL, VIOLET, INDIGO, ROSE, CORAL, AMBER, SLATE
}

object ThemeState {
    private const val PREFS_NAME = "theme_settings"
    private const val KEY_PALETTE = "palette"
    private const val KEY_SURFACE_PALETTE = "surface_palette"
    private const val KEY_DARK_THEME = "dark_theme"

    val currentPalette = mutableStateOf<ThemePalette?>(null)
    val surfacePalette = mutableStateOf<ThemePalette?>(null)
    val isDarkTheme = mutableStateOf(false)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentPalette.value = prefs.getString(KEY_PALETTE, null)?.let { runCatching { ThemePalette.valueOf(it) }.getOrNull() }
        surfacePalette.value = prefs.getString(KEY_SURFACE_PALETTE, null)?.let { runCatching { ThemePalette.valueOf(it) }.getOrNull() }
        isDarkTheme.value = prefs.getBoolean(KEY_DARK_THEME, false)
    }

    fun setPalette(context: Context, palette: ThemePalette?) {
        currentPalette.value = palette
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            if (palette == null) remove(KEY_PALETTE) else putString(KEY_PALETTE, palette.name)
        }
    }

    fun setSurfacePalette(context: Context, palette: ThemePalette?) {
        surfacePalette.value = palette
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            if (palette == null) remove(KEY_SURFACE_PALETTE) else putString(KEY_SURFACE_PALETTE, palette.name)
        }
    }

    fun setDarkTheme(context: Context, enabled: Boolean) {
        isDarkTheme.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_DARK_THEME, enabled)
        }
    }
}

private val DarkEmerald = darkColorScheme(
    primary = EmeraldPrimaryDark,
    secondary = EmeraldSecondaryDark,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF2D3142)
)

private val LightEmerald = lightColorScheme(
    primary = EmeraldPrimaryLight,
    secondary = EmeraldSecondaryLight,
    background = LightBg,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0)
)

private val DarkViolet = darkColorScheme(
    primary = VioletPrimaryDark,
    secondary = VioletSecondaryDark,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF2D3142)
)

private val LightViolet = lightColorScheme(
    primary = VioletPrimaryLight,
    secondary = VioletSecondaryLight,
    background = LightBg,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0)
)

private val DarkCoral = darkColorScheme(
    primary = CoralPrimaryDark,
    secondary = CoralSecondaryDark,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF2D3142)
)

private val LightCoral = lightColorScheme(
    primary = CoralPrimaryLight,
    secondary = CoralSecondaryLight,
    background = LightBg,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0)
)

private val DarkCyan = darkColorScheme(
    primary = CyanPrimaryDark,
    secondary = CyanSecondaryDark,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF2D3142)
)

private val LightCyan = lightColorScheme(
    primary = CyanPrimaryLight,
    secondary = CyanSecondaryLight,
    background = LightBg,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0)
)

@Composable
fun animateColorScheme(targetColorScheme: ColorScheme): ColorScheme {
    val duration = 400
    val primary = animateColorAsState(targetColorScheme.primary, animationSpec = tween(duration), label = "primary").value
    val secondary = animateColorAsState(targetColorScheme.secondary, animationSpec = tween(duration), label = "secondary").value
    val background = animateColorAsState(targetColorScheme.background, animationSpec = tween(duration), label = "background").value
    val surface = animateColorAsState(targetColorScheme.surface, animationSpec = tween(duration), label = "surface").value
    val onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec = tween(duration), label = "onPrimary").value
    val onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec = tween(duration), label = "onSecondary").value
    val onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec = tween(duration), label = "onBackground").value
    val onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec = tween(duration), label = "onSurface").value
    val surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec = tween(duration), label = "surfaceVariant").value

    return targetColorScheme.copy(
        primary = primary,
        secondary = secondary,
        background = background,
        surface = surface,
        onPrimary = onPrimary,
        onSecondary = onSecondary,
        onBackground = onBackground,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant
    )
}

@Composable
fun CaloriesCounterTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = ThemeState.isDarkTheme.value
    val palette = ThemeState.currentPalette.value
    val surfacePalette = ThemeState.surfacePalette.value

    val baseScheme = colorScheme(
        primaryPalette = palette,
        surfacePalette = surfacePalette,
        darkTheme = darkTheme
    )

    val animatedScheme = animateColorScheme(baseScheme)

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = Typography,
        content = content
    )
}

private fun colorScheme(
    primaryPalette: ThemePalette?,
    surfacePalette: ThemePalette?,
    darkTheme: Boolean
): ColorScheme {
    val defaultPalette = ThemePalette.CYAN
    val (_, defaultSecondary) = defaultPalette.paletteColors(darkTheme)
    val (selectedPrimary, _) = (primaryPalette ?: defaultPalette).paletteColors(darkTheme)
    val primary = selectedPrimary
    val secondary = defaultSecondary
    val surfaceVariant = surfacePalette?.paletteColors(darkTheme)?.second?.let { surfaceTint ->
        if (darkTheme) {
            lerp(DarkSurface, surfaceTint, 0.26f)
        } else {
            lerp(Color.White, surfaceTint, 0.11f)
        }
    } ?: if (darkTheme) Color(0xFF343849) else Color(0xFFF1F5F9)

    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            secondary = secondary,
            background = DarkBg,
            surface = DarkSurface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFFE2E8F0),
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = surfaceVariant
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = secondary,
            background = LightBg,
            surface = LightSurface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF1E293B),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = surfaceVariant
        )
    }
}

private fun ThemePalette.paletteColors(darkTheme: Boolean): Pair<Color, Color> {
    return when (this) {
        ThemePalette.CYAN -> if (darkTheme) CyanPrimaryDark to CyanSecondaryDark else CyanPrimaryLight to CyanSecondaryLight
        ThemePalette.EMERALD -> if (darkTheme) EmeraldPrimaryDark to EmeraldSecondaryDark else EmeraldPrimaryLight to EmeraldSecondaryLight
        ThemePalette.TEAL -> if (darkTheme) TealPrimaryDark to TealSecondaryDark else TealPrimaryLight to TealSecondaryLight
        ThemePalette.VIOLET -> if (darkTheme) VioletPrimaryDark to VioletSecondaryDark else VioletPrimaryLight to VioletSecondaryLight
        ThemePalette.INDIGO -> if (darkTheme) IndigoPrimaryDark to IndigoSecondaryDark else IndigoPrimaryLight to IndigoSecondaryLight
        ThemePalette.ROSE -> if (darkTheme) RosePrimaryDark to RoseSecondaryDark else RosePrimaryLight to RoseSecondaryLight
        ThemePalette.CORAL -> if (darkTheme) CoralPrimaryDark to CoralSecondaryDark else CoralPrimaryLight to CoralSecondaryLight
        ThemePalette.AMBER -> if (darkTheme) AmberPrimaryDark to AmberSecondaryDark else AmberPrimaryLight to AmberSecondaryLight
        ThemePalette.SLATE -> if (darkTheme) SlatePrimaryDark to SlateSecondaryDark else SlatePrimaryLight to SlateSecondaryLight
    }
}

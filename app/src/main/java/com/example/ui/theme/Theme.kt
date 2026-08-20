package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val HighContrastEnterpriseColorScheme = lightColorScheme(
    primary = Slate900,
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate900,
    secondary = Slate800,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate800,
    tertiary = AmberWarning,
    onTertiary = Color.White,
    tertiaryContainer = AmberSurface,
    onTertiaryContainer = AmberText,
    background = Slate50,
    onBackground = Slate950,
    surface = SurfaceWhite,
    onSurface = Slate950,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate800,
    surfaceTint = Color.Transparent,
    inverseSurface = Slate900,
    inverseOnSurface = Color.White,
    outline = Slate300,
    outlineVariant = Slate200,
    error = RoseDanger,
    onError = Color.White,
    errorContainer = RoseSurface,
    onErrorContainer = RoseText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Always enforce crisp, high-contrast Enterprise Light Theme for SFA
    content: @Composable () -> Unit
) {
    val colorScheme = HighContrastEnterpriseColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Slate900
        ) {
            Surface(
                color = Slate50,
                contentColor = Slate900
            ) {
                content()
            }
        }
    }
}

@Composable
fun appTextFieldColors(
    focusedContainerColor: Color = Color.White,
    unfocusedContainerColor: Color = Color.White,
    focusedBorderColor: Color = Slate900,
    unfocusedBorderColor: Color = Slate300,
    focusedTextColor: Color = Slate950,
    unfocusedTextColor: Color = Slate900
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = focusedTextColor,
    unfocusedTextColor = unfocusedTextColor,
    disabledTextColor = Slate500,
    focusedLabelColor = Slate900,
    unfocusedLabelColor = Slate600,
    disabledLabelColor = Slate400,
    focusedPlaceholderColor = Slate400,
    unfocusedPlaceholderColor = Slate400,
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    focusedContainerColor = focusedContainerColor,
    unfocusedContainerColor = unfocusedContainerColor,
    disabledContainerColor = Slate100,
    cursorColor = Slate900,
    errorTextColor = RoseDanger,
    errorBorderColor = RoseDanger,
    errorLabelColor = RoseDanger,
    errorContainerColor = Color.White,
    focusedLeadingIconColor = Slate800,
    unfocusedLeadingIconColor = Slate500,
    focusedTrailingIconColor = Slate800,
    unfocusedTrailingIconColor = Slate500
)


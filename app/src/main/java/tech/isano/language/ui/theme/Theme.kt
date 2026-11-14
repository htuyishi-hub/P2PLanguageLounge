package tech.isano.language.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AccessibilityColorScheme = darkColorScheme(
    primary = WarmCoral,
    onPrimary = PureWhite,
    primaryContainer = WarmCoral,
    onPrimaryContainer = PureWhite,
    secondary = WarmCoral,
    onSecondary = PureWhite,
    background = ObsidianNavy,
    onBackground = PureWhite,
    surface = ElevatedNavy,
    onSurface = PureWhite,
    surfaceVariant = ElevatedNavy,
    onSurfaceVariant = MutedSlateGrey,
    outline = MutedSlateGrey
)

@Composable
fun P2PLanguageLoungeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AccessibilityColorScheme,
        typography = Typography,
        content = content
    )
}

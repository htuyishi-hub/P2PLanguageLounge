package tech.isano.language.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tech.isano.language.ui.theme.ObsidianNavy
import tech.isano.language.ui.theme.PureWhite
import tech.isano.language.ui.theme.WarmCoral

/**
 * Magical loading experience replacing generic CircularProgressIndicator.
 * Engages the user via sequential, cinematic transitions ("The Illusion of Labor").
 *
 * Cycles every 2000ms:
 * - Step 0: Icons.Default.Public -> "Connecting world..."
 * - Step 1: Icons.Default.EnhancedEncryption -> "Securing keys..."
 * - Step 2: Icons.Default.Weekend -> "Setting up space..."
 */
@Composable
fun MagicLoadingOverlay(
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            step = (step + 1) % 3
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNavy),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 600)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 600))
            },
            label = "magic_loading_step"
        ) { currentStep ->
            val stepData = when (currentStep) {
                0 -> StepConfig(
                    icon = Icons.Default.Public,
                    contentDescription = "Connecting world",
                    text = "Connecting world..."
                )
                1 -> StepConfig(
                    icon = Icons.Default.EnhancedEncryption,
                    contentDescription = "Securing keys",
                    text = "Securing keys..."
                )
                else -> StepConfig(
                    icon = Icons.Default.Weekend,
                    contentDescription = "Setting up space",
                    text = "Setting up space..."
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = stepData.icon,
                    contentDescription = stepData.contentDescription,
                    tint = WarmCoral,
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stepData.text,
                    color = PureWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class StepConfig(
    val icon: ImageVector,
    val contentDescription: String,
    val text: String
)

package tech.isano.language.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.isano.language.ui.NavTab
import tech.isano.language.ui.theme.ElevatedNavy
import tech.isano.language.ui.theme.MutedSlateGrey
import tech.isano.language.ui.theme.ObsidianNavy
import tech.isano.language.ui.theme.PureWhite
import tech.isano.language.ui.theme.WarmCoral

/**
 * Sovereign, Zero-Reading Onboarding Flow.
 *
 * Replaces centralized Google Authentication with a 2-step Cryptographic Lockbox flow:
 * - Step 1 (Username): Clean, massive text field to enter a sovereign handle (e.g. isano.tech style).
 * - Step 2 (The Vault): Custom, massive visual numeric keypad (0-9) filling the bottom half.
 *   Strictly no system keyboard. The user taps 4 digits to lock/unlock their account.
 * - Step 3 (Processing): The shifting "Illusion of Labor" icons mask background AES-GCM
 *   encryption and Jami OpenDHT Swarm generation.
 */
enum class OnboardingStep {
    USERNAME,
    VAULT,
    PROCESSING
}

@Composable
fun OnboardingScreen(
    onComplete: (initialTab: NavTab) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel()
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.USERNAME) }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val isAuthenticating by viewModel.isAuthenticating.collectAsState()

    AnimatedContent(
        targetState = if (isAuthenticating || currentStep == OnboardingStep.PROCESSING) {
            OnboardingStep.PROCESSING
        } else {
            currentStep
        },
        transitionSpec = {
            if (targetState == OnboardingStep.PROCESSING || initialState == OnboardingStep.PROCESSING) {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "onboarding_vault_flow"
    ) { step ->
        when (step) {
            OnboardingStep.USERNAME -> {
                UsernameStep(
                    username = username,
                    onUsernameChange = {
                        username = it
                        localError = null
                    },
                    onContinue = {
                        if (username.trim().isNotEmpty()) {
                            pin = ""
                            localError = null
                            currentStep = OnboardingStep.VAULT
                        }
                    },
                    modifier = modifier
                )
            }

            OnboardingStep.VAULT -> {
                VaultPinStep(
                    username = username.trim(),
                    pin = pin,
                    errorMessage = localError,
                    onDigitPress = { digit ->
                        if (pin.length < 4) {
                            val nextPin = pin + digit
                            pin = nextPin
                            localError = null
                            if (nextPin.length == 4) {
                                currentStep = OnboardingStep.PROCESSING
                                viewModel.authenticateWithVault(
                                    username = username.trim(),
                                    pin = nextPin,
                                    onSuccess = {
                                        onComplete(NavTab.LOUNGE)
                                    },
                                    onError = { err ->
                                        localError = err
                                        pin = ""
                                        currentStep = OnboardingStep.VAULT
                                    }
                                )
                            }
                        }
                    },
                    onBackspacePress = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                            localError = null
                        }
                    },
                    onClearPress = {
                        pin = ""
                        localError = null
                    },
                    onBack = {
                        currentStep = OnboardingStep.USERNAME
                    },
                    modifier = modifier
                )
            }

            OnboardingStep.PROCESSING -> {
                MagicLoadingOverlay(modifier = modifier)
            }
        }
    }
}

// -------------------------------------------------------------------------
// Step 1: USERNAME (Massive Clean Handle Input)
// -------------------------------------------------------------------------

@Composable
private fun UsernameStep(
    username: String,
    onUsernameChange: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNavy)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language Lounge",
                    tint = WarmCoral,
                    modifier = Modifier.size(96.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "LANGUAGE LOUNGE",
                    color = PureWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sovereign, Zero-Server Language Exchange",
                    color = MutedSlateGrey,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Input Section
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Choose Your Handle",
                    color = PureWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your username to access or create your sovereign P2P vault",
                    color = MutedSlateGrey,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { input ->
                        onUsernameChange(input.filter { it.isLetterOrDigit() || it == '_' || it == '.' })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                    textStyle = TextStyle(
                        color = PureWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    placeholder = {
                        Text(
                            text = "username",
                            color = MutedSlateGrey,
                            fontSize = 22.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AlternateEmail,
                            contentDescription = null,
                            tint = WarmCoral,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmCoral,
                        unfocusedBorderColor = ElevatedNavy,
                        focusedContainerColor = ElevatedNavy,
                        unfocusedContainerColor = ElevatedNavy,
                        cursorColor = WarmCoral
                    )
                )
            }

            // Continue Button (Height 88dp)
            Button(
                onClick = onContinue,
                enabled = username.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarmCoral,
                    contentColor = PureWhite,
                    disabledContainerColor = ElevatedNavy.copy(alpha = 0.5f),
                    disabledContentColor = MutedSlateGrey
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "ENTER VAULT",
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Step 2: THE VAULT (Custom Visual Keypad Filling Bottom Half)
// -------------------------------------------------------------------------

@Composable
private fun VaultPinStep(
    username: String,
    pin: String,
    errorMessage: String?,
    onDigitPress: (String) -> Unit,
    onBackspacePress: () -> Unit,
    onClearPress: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNavy)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Half: The Vault Lock Header & Indicators
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = WarmCoral,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "THE VAULT",
                    color = PureWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "@$username",
                    color = WarmCoral,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tap 4 digits to lock or unlock your account",
                    color = MutedSlateGrey,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4 PIN Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) WarmCoral else ElevatedNavy)
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage,
                        color = WarmCoral,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Half: Massive Visual Numeric Keypad (Fills bottom half, no system keyboard)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "BACK")
                )

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(66.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ElevatedNavy)
                                    .clickable {
                                        when (key) {
                                            "BACK" -> onBackspacePress()
                                            "C" -> onClearPress()
                                            else -> onDigitPress(key)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "BACK" -> {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = PureWhite,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    "C" -> {
                                        Text(
                                            text = "C",
                                            color = WarmCoral,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = key,
                                            color = PureWhite,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

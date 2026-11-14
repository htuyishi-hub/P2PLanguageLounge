package tech.isano.language.ui.lounge

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import tech.isano.language.lounge.LoungeUiState
import tech.isano.language.lounge.LoungeViewModel
import tech.isano.language.ui.theme.ElevatedNavy
import tech.isano.language.ui.theme.MutedSlateGrey
import tech.isano.language.ui.theme.ObsidianNavy
import tech.isano.language.ui.theme.PureWhite
import tech.isano.language.ui.theme.WarmCoral

/**
 * Live Lounge tab: Zero-reading, face-to-face drop-in video radio interface.
 * Connects automatically to the OpenDHT frequency with no phone dials or text.
 * Integrates a Visual Permission Primer requesting both Camera and Microphone.
 */
@Composable
fun LiveLoungeTab(
    viewModel: LoungeViewModel,
    onOpenCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var isPermissionPrimerActive by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionPrimerActive = false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true

        if (audioGranted && cameraGranted) {
            viewModel.joinLounge()
        } else {
            viewModel.resetFromPrimer()
        }
    }

    val requestLoungeEntry = {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission && hasCameraPermission) {
            viewModel.joinLounge()
        } else {
            isPermissionPrimerActive = true
            viewModel.showPermissionPrimer()
        }
    }

    if (isPermissionPrimerActive || uiState is LoungeUiState.PermissionPrimer) {
        PermissionPrimerScreen(
            onGrantPermissionClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                    )
                )
            },
            modifier = modifier
        )
    } else {
        LiveLoungeTabContent(
            uiState = uiState,
            onJoinLobby = requestLoungeEntry,
            onLeaveLobby = { viewModel.leaveLobby() },
            onOpenCanvas = onOpenCanvas,
            modifier = modifier
        )
    }
}

/**
 * Full-screen Visual Permission Primer for Camera & Microphone.
 * Massive 150dp pulsing WarmCoral Videocam button with clear visual guidance.
 */
@Composable
private fun PermissionPrimerScreen(
    onGrantPermissionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "video_pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "video_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Halo background
            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .graphicsLayer { alpha = haloAlpha }
                        .clip(CircleShape)
                        .background(WarmCoral.copy(alpha = 0.35f))
                )

                // Giant Pulsing WarmCoral Videocam Target (size 150dp)
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(WarmCoral)
                        .clickable(onClick = onGrantPermissionClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Turn on camera & mic",
                        tint = PureWhite,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Tap to turn on your camera & mic",
                color = PureWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LiveLoungeTabContent(
    uiState: LoungeUiState,
    onJoinLobby: () -> Unit,
    onLeaveLobby: () -> Unit,
    onOpenCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 4. Connected State: Automatic instant bridge into Face-to-Face Video Canvas session
    LaunchedEffect(uiState) {
        if (uiState is LoungeUiState.Connected) {
            onOpenCanvas()
        }
    }

    val isScanning = uiState is LoungeUiState.Scanning
    val isListening = uiState is LoungeUiState.RoomEmpty_Listening
    val interactionSource = remember { MutableInteractionSource() }

    // Radar infinite transition for scanning wave ripples
    val radarTransition = rememberInfiniteTransition(label = "radar_transition")

    val wave1 by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val wave2 by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    val wave3 by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3"
    )

    // Breathing pulse for quiet room listening state
    val breathingTransition = rememberInfiniteTransition(label = "breathing_transition")
    val breathingAlpha by breathingTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNavy),
        contentAlignment = Alignment.Center
    ) {
        // 2. Scanning State: Concentric radar circles expanding from 1f to 2.2f with alpha fade
        if (isScanning) {
            listOf(wave1, wave2, wave3).forEach { waveProgress ->
                val waveScale = 1f + (waveProgress * 1.2f)
                val waveAlpha = (1f - waveProgress) * 0.5f

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(waveScale)
                        .graphicsLayer { alpha = waveAlpha }
                        .clip(CircleShape)
                        .background(WarmCoral)
                )
            }
        }

        // 3. RoomEmpty_Listening State: Ambient breathing halo
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .graphicsLayer { alpha = breathingAlpha }
                    .clip(CircleShape)
                    .border(BorderStroke(2.dp, WarmCoral.copy(alpha = breathingAlpha)), CircleShape)
            )
        }

        // Central Action Button (Visual-first touch target)
        when (uiState) {
            LoungeUiState.Idle, LoungeUiState.PermissionPrimer -> {
                // 1. Idle State: Massive WarmCoral button (size 150dp), CellTower icon (size 80dp)
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(WarmCoral)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onJoinLobby
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Drop In",
                        tint = PureWhite,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            LoungeUiState.Scanning -> {
                // Scanning: Active pulsing anchor with cancel affordance on tap
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(WarmCoral)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onLeaveLobby
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Scanning",
                        tint = PureWhite,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            LoungeUiState.RoomEmpty_Listening -> {
                // 3. RoomEmpty_Listening State: Shrunk slightly (130dp), ElevatedNavy, MutedSlateGrey border, Hearing icon
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(ElevatedNavy)
                        .border(BorderStroke(3.dp, MutedSlateGrey), CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onLeaveLobby
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Listening",
                        tint = WarmCoral,
                        modifier = Modifier.size(70.dp)
                    )
                }
            }

            is LoungeUiState.Connected -> {
                // Connected State: Immediate bridge indicator
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(WarmCoral),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Connected",
                        tint = PureWhite,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }
    }
}

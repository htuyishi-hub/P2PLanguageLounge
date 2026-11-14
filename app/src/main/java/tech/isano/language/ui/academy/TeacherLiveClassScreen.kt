package tech.isano.language.ui.academy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.isano.language.ui.theme.ElevatedNavy
import tech.isano.language.ui.theme.MutedSlateGrey
import tech.isano.language.ui.theme.ObsidianNavy
import tech.isano.language.ui.theme.PureWhite
import tech.isano.language.ui.theme.WarmCoral

data class SwarmStudent(
    val id: String,
    val flagEmoji: String,
    val isSpeaking: Boolean = false
)

/**
 * Teacher Live Class Screen: Collaborative Jami Swarm teaching interface.
 * Features an interactive master whiteboard, floating student PIP row,
 * and a 4-tool bottom command palette with zero text.
 */
@Composable
fun TeacherLiveClassScreen(
    onEndClass: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Whiteboard drawing state
    val masterPath = remember { Path() }
    var motionTick by remember { mutableLongStateOf(0L) }

    // Tool states
    var isDrawingActive by remember { mutableStateOf(true) }
    var showFlashcard by remember { mutableStateOf(false) }
    var isRoomMuted by remember { mutableStateOf(false) }
    var isPraiseFlashing by remember { mutableStateOf(false) }

    // Animated Praise burst scale
    val praiseScale = remember { Animatable(0f) }

    // Connected Students in Jami Swarm
    val students = remember {
        listOf(
            SwarmStudent("s1", "🇷🇼", isSpeaking = true),
            SwarmStudent("s2", "🇫🇷", isSpeaking = false),
            SwarmStudent("s3", "🇺🇸", isSpeaking = false),
            SwarmStudent("s4", "🇪🇸", isSpeaking = true),
            SwarmStudent("s5", "🇰🇪", isSpeaking = false)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNavy)
    ) {
        // -----------------------------------------------------------------
        // Layer 1: Background Interactive Master Whiteboard
        // -----------------------------------------------------------------
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isDrawingActive) {
                    if (isDrawingActive) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                masterPath.moveTo(offset.x, offset.y)
                                motionTick++
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                masterPath.lineTo(change.position.x, change.position.y)
                                motionTick++
                            }
                        )
                    }
                }
        ) {
            motionTick.let { _ ->
                drawPath(
                    path = masterPath,
                    color = WarmCoral,
                    style = Stroke(
                        width = 16f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Visual Flashcard Overlay (Pushed to students when Tool 2 is active)
        AnimatedVisibility(
            visible = showFlashcard,
            enter = scaleIn(tween(300)) + fadeIn(),
            exit = scaleOut(tween(300)) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier
                    .size(240.dp)
                    .clickable { showFlashcard = false },
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = ElevatedNavy),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🍎", // Visual flashcard image
                        fontSize = 110.sp
                    )
                }
            }
        }

        // Praise / Star Reward Burst Animation
        if (praiseScale.value > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .scale(praiseScale.value),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Praise Star",
                    tint = WarmCoral,
                    modifier = Modifier.size(160.dp)
                )
            }
        }

        // -----------------------------------------------------------------
        // Layer 2: Top Bar with massive 'End Class' Button
        // -----------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(WarmCoral)
                    .clickable(onClick = onEndClass),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Class",
                    tint = PureWhite,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // -----------------------------------------------------------------
        // Layer 3: Student Grid (Picture-in-Picture above bottom bar)
        // -----------------------------------------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 116.dp)
                .fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(students, key = { it.id }) { student ->
                    StudentAvatarPip(student = student)
                }
            }
        }

        // -----------------------------------------------------------------
        // Layer 4: The Teacher Tool Palette (Bottom Bar)
        // -----------------------------------------------------------------
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedNavy),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tool 1: Draw (Whiteboard Toggle)
                TeacherToolButton(
                    icon = Icons.Default.Brush,
                    tint = if (isDrawingActive) WarmCoral else PureWhite,
                    isActive = isDrawingActive,
                    onClick = { isDrawingActive = !isDrawingActive }
                )

                // Tool 2: Flashcard/Image (Push picture to students)
                TeacherToolButton(
                    icon = Icons.Default.Image,
                    tint = if (showFlashcard) WarmCoral else PureWhite,
                    isActive = showFlashcard,
                    onClick = { showFlashcard = !showFlashcard }
                )

                // Tool 3: Megaphone / Audio Command (Mute All Students)
                TeacherToolButton(
                    icon = if (isRoomMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    tint = if (isRoomMuted) WarmCoral else PureWhite,
                    isActive = isRoomMuted,
                    onClick = { isRoomMuted = !isRoomMuted }
                )

                // Tool 4: Praise / Star (Reward Students)
                TeacherToolButton(
                    icon = Icons.Default.Star,
                    tint = if (isPraiseFlashing) WarmCoral else PureWhite,
                    isActive = isPraiseFlashing,
                    onClick = {
                        coroutineScope.launch {
                            isPraiseFlashing = true
                            praiseScale.snapTo(0.2f)
                            praiseScale.animateTo(
                                targetValue = 1.3f,
                                animationSpec = tween(500, easing = FastOutSlowInEasing)
                            )
                            delay(400)
                            praiseScale.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(300)
                            )
                            isPraiseFlashing = false
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Helper: 72dp Massive Teacher Tool Button
// -------------------------------------------------------------------------

@Composable
private fun TeacherToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (isActive) WarmCoral.copy(alpha = 0.2f) else ElevatedNavy)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(36.dp)
        )
    }
}

// -------------------------------------------------------------------------
// Helper: 64dp Floating Student Picture-in-Picture Avatar
// -------------------------------------------------------------------------

@Composable
private fun StudentAvatarPip(
    student: SwarmStudent
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(ElevatedNavy)
            .then(
                if (student.isSpeaking) {
                    Modifier.border(3.dp, WarmCoral, CircleShape)
                } else {
                    Modifier.border(2.dp, MutedSlateGrey, CircleShape)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = student.flagEmoji,
            fontSize = 30.sp
        )
    }
}

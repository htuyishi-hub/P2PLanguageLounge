package tech.isano.language.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.isano.language.ui.theme.ElevatedNavy
import tech.isano.language.ui.theme.MutedSlateGrey
import tech.isano.language.ui.theme.ObsidianNavy
import tech.isano.language.ui.theme.PureWhite
import tech.isano.language.ui.theme.WarmCoral

data class ChatFriend(
    val id: String,
    val flagEmoji: String,
    val avatarColor: Long,
    val isOnline: Boolean = true
)

data class VoiceMessage(
    val id: String,
    val isSent: Boolean,
    val isPlaying: Boolean = false,
    val isTranslated: Boolean = false
)

/**
 * Chat Tab: Voice-First, Zero-Reading Push-to-Talk Walkie-Talkie interface.
 * Delegates to [CommunityChatTab] for decentralized P2P voice messaging.
 */
@Composable
fun ChatTab(
    modifier: Modifier = Modifier
) {
    CommunityChatTab(modifier = modifier)
}

/**
 * Preview / mock layout for ChatTab standalone testing.
 */
@Composable
fun ChatTabMockContent(
    modifier: Modifier = Modifier
) {
    // Mock Active Friends list (recent peers with flag emojis)
    val friends = remember {
        listOf(
            ChatFriend("1", "🇷🇼", 0xFF10B981),
            ChatFriend("2", "🇫🇷", 0xFF3B82F6),
            ChatFriend("3", "🇺🇸", 0xFFEC4899),
            ChatFriend("4", "🇪🇸", 0xFFF59E0B),
            ChatFriend("5", "🇯🇵", 0xFF8B5CF6)
        )
    }

    // Voice Message Feed
    val messages = remember {
        mutableStateListOf(
            VoiceMessage("m1", isSent = false),
            VoiceMessage("m2", isSent = true),
            VoiceMessage("m3", isSent = false),
            VoiceMessage("m4", isSent = true)
        )
    }

    var isRecording by remember { mutableStateOf(false) }

    // Pulsing recording animation for Push-to-Talk button
    val recordTransition = rememberInfiniteTransition(label = "recording_pulse")
    val recordingScale by recordTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recording_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNavy)
    ) {
        // -----------------------------------------------------------------
        // Top Section: Active Friends (Height 100dp, ElevatedNavy background)
        // -----------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(ElevatedNavy)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(friends, key = { it.id }) { friend ->
                    ActiveFriendAvatar(friend = friend)
                }
            }
        }

        // -----------------------------------------------------------------
        // Middle Section: Voice Feed (Chat history, no text)
        // -----------------------------------------------------------------
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            reverseLayout = false
        ) {
            items(messages, key = { it.id }) { message ->
                MockVoiceMessageBubble(
                    message = message,
                    onPlayToggle = {
                        val index = messages.indexOf(message)
                        if (index != -1) {
                            messages[index] = message.copy(isPlaying = !message.isPlaying)
                        }
                    },
                    onTranslateToggle = {
                        val index = messages.indexOf(message)
                        if (index != -1) {
                            messages[index] = message.copy(isTranslated = !message.isTranslated)
                        }
                    }
                )
            }
        }

        // -----------------------------------------------------------------
        // Bottom Section: Push-to-Talk Walkie-Talkie (Height 120dp)
        // -----------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(ObsidianNavy),
            contentAlignment = Alignment.Center
        ) {
            // Giant WarmCoral Push-to-Talk Button (size 90dp)
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(recordingScale)
                    .clip(CircleShape)
                    .background(WarmCoral)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isRecording = true
                                val released = tryAwaitRelease()
                                isRecording = false
                                if (released) {
                                    // Add new voice note to feed
                                    messages.add(
                                        VoiceMessage(
                                            id = "m_${System.currentTimeMillis()}",
                                            isSent = true
                                        )
                                    )
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Hold to Talk",
                    tint = PureWhite,
                    modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Active Friend Circular Avatar Component (72dp)
// -------------------------------------------------------------------------

@Composable
private fun ActiveFriendAvatar(
    friend: ChatFriend
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(ObsidianNavy)
            .clickable { /* Select friend conversation */ },
        contentAlignment = Alignment.Center
    ) {
        // Avatar circle with country flag emoji
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(ElevatedNavy),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = friend.flagEmoji,
                fontSize = 32.sp
            )
        }

        // Online Status Dot
        if (friend.isOnline) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(WarmCoral)
            )
        }
    }
}

// -------------------------------------------------------------------------
// Voice Note Bubble Component (Received = ElevatedNavy, Sent = WarmCoral)
// -------------------------------------------------------------------------

@Composable
fun MockVoiceMessageBubble(
    message: VoiceMessage,
    onPlayToggle: () -> Unit,
    onTranslateToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSent = message.isSent
    val bubbleColor = if (isSent) WarmCoral else ElevatedNavy

    // Align Sent to end, Received to start
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(84.dp), // Massive touch target
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = if (isSent) 24.dp else 4.dp,
                bottomEnd = if (isSent) 4.dp else 24.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Large Play / Pause Action Button (size 40dp, PureWhite)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlayToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (message.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Voice Note",
                        tint = PureWhite,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Middle: Static Visual Waveform (Row of vertical bars with 0.7f alpha)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barHeights = listOf(14.dp, 32.dp, 44.dp, 24.dp, 40.dp, 18.dp, 36.dp, 20.dp)
                    barHeights.forEach { height ->
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(height)
                                .clip(RoundedCornerShape(3.dp))
                                .background(PureWhite.copy(alpha = 0.7f))
                        )
                    }
                }

                // Right Side: Large Translation Toggle Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (message.isTranslated) PureWhite.copy(alpha = 0.25f) else bubbleColor)
                        .clickable(onClick = onTranslateToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Translate Voice Note",
                        tint = PureWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

package tech.isano.language.ui.chat

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tech.isano.language.ui.theme.ElevatedNavy
import tech.isano.language.ui.theme.PureWhite
import tech.isano.language.ui.theme.WarmCoral
import java.io.File

/**
 * Audio Playback Bubble for P2P Walkie-Talkie voice notes.
 *
 * Designed with a zero-reading, voice-first approach:
 * - Massive 80dp touch target.
 * - Prominent Play/Pause button and visual waveform bars.
 * - Distinct color-coding and alignment (WarmCoral for local, ElevatedNavy for remote).
 */
@Composable
fun VoiceMessageBubble(
    file: File,
    isSentByMe: Boolean,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }

    // Clean up MediaPlayer when the bubble leaves composition
    DisposableEffect(file) {
        mediaPlayer.setOnCompletionListener {
            isPlaying = false
        }
        mediaPlayer.setOnErrorListener { _, _, _ ->
            isPlaying = false
            true
        }

        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    val bubbleColor = if (isSentByMe) WarmCoral else ElevatedNavy
    val horizontalArrangement = if (isSentByMe) Arrangement.End else Arrangement.Start

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(80.dp),
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = if (isSentByMe) 24.dp else 6.dp,
                bottomEnd = if (isSentByMe) 6.dp else 24.dp
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
                // Left side: Massive Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.2f))
                        .clickable {
                            if (isPlaying) {
                                try {
                                    mediaPlayer.pause()
                                    isPlaying = false
                                } catch (_: Exception) {
                                    isPlaying = false
                                }
                            } else {
                                try {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(file.absolutePath)
                                    mediaPlayer.prepare()
                                    mediaPlayer.start()
                                    isPlaying = true
                                } catch (_: Exception) {
                                    isPlaying = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Voice Note" else "Play Voice Note",
                        tint = PureWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Center: Visual waveform / equalizer bars
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barHeights = listOf(
                        14.dp, 28.dp, 44.dp, 20.dp, 48.dp, 56.dp,
                        32.dp, 42.dp, 18.dp, 38.dp, 24.dp, 12.dp
                    )
                    barHeights.forEachIndexed { index, height ->
                        val barAlpha = if (isPlaying) {
                            if (index % 2 == 0) 0.95f else 0.75f
                        } else {
                            0.65f
                        }
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(height)
                                .clip(RoundedCornerShape(3.dp))
                                .background(PureWhite.copy(alpha = barAlpha))
                        )
                    }
                }
            }
        }
    }
}

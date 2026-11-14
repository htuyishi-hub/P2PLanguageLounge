package tech.isano.language.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.isano.language.core.engine.audio.VoiceNoteRecorder
import tech.isano.language.core.engine.jami.JamiDaemon
import tech.isano.language.core.engine.jami.JamiDaemonImpl
import tech.isano.language.ui.theme.ElevatedNavy
import tech.isano.language.ui.theme.ObsidianNavy
import tech.isano.language.ui.theme.PureWhite
import tech.isano.language.ui.theme.WarmCoral
import java.io.File

/**
 * Community Chat Tab: Decentralized, asynchronous Walkie-Talkie voice messaging.
 *
 * ZERO-READING UX:
 * - Uses a massive 120dp "Hold to Speak" button powered by gesture detection.
 * - Streams recorded audio files directly over Jami Swarms via [VoiceNoteRecorder].
 * - Incoming audio files are received over Jami Swarm channels and rendered in the message feed
 *   using [VoiceMessageBubble] with native MediaPlayer playback.
 */
@Composable
fun CommunityChatTab(
    modifier: Modifier = Modifier,
    jamiDaemon: JamiDaemon = remember { JamiDaemonImpl() }
) {
    val context = LocalContext.current
    val voiceRecorder = remember { VoiceNoteRecorder(context) }
    var isHoldingToSpeak by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf<Pair<File, Boolean>>()
    }

    // Listen for incoming audio files from peers over the Jami Swarm
    LaunchedEffect(jamiDaemon) {
        jamiDaemon.incomingFiles.collect { filePath ->
            val file = File(filePath)
            if (file.exists() && (filePath.endsWith(".aac") || filePath.endsWith(".m4a"))) {
                messages.add(file to false)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNavy)
    ) {
        // Message Feed rendered via LazyColumn and VoiceMessageBubble
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { (file, isSentByMe) ->
                VoiceMessageBubble(
                    file = file,
                    isSentByMe = isSentByMe
                )
            }
        }

        // Bottom Section: Massive 120dp "Hold to Speak" Walkie-Talkie Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(ObsidianNavy)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (isHoldingToSpeak) WarmCoral else ElevatedNavy)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                var outputFile: File? = null
                                try {
                                    isHoldingToSpeak = true
                                    outputFile = voiceRecorder.startRecording()
                                    tryAwaitRelease()
                                } finally {
                                    isHoldingToSpeak = false
                                    voiceRecorder.stopRecording()
                                    outputFile?.let { file ->
                                        if (file.exists() && file.length() > 0) {
                                            jamiDaemon.sendFileToSwarm(file.absolutePath)
                                            messages.add(file to true)
                                        }
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isHoldingToSpeak) "Recording" else "Hold to speak",
                        tint = if (isHoldingToSpeak) PureWhite else WarmCoral,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (isHoldingToSpeak) "Recording..." else "Hold to Speak",
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

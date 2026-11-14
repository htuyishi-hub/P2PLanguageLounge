package tech.isano.language.feature.canvas.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import tech.isano.language.core.engine.jami.JamiDaemon
import tech.isano.language.core.engine.jami.JamiDaemonImpl
import java.io.File

// Strict High-Contrast Theme Palette
private val ObsidianNavy = Color(0xFF070D19)
private val ElevatedNavy = Color(0xFF0F1E36)
private val WarmCoral = Color(0xFFFF6B4A)
private val PureWhite = Color(0xFFFFFFFF)

/**
 * Protocol representation of canvas drawing actions over the Jami Data Channel.
 * Normalizes coordinates into relative units (0.0f .. 1.0f) for cross-device resolution parity.
 */
sealed class CanvasEvent {
    data class MoveTo(val x: Float, val y: Float) : CanvasEvent()
    data class LineTo(val x: Float, val y: Float) : CanvasEvent()
    data object Clear : CanvasEvent()

    fun toJson(): String = when (this) {
        is MoveTo -> "{\"type\":\"MOVE\",\"x\":$x,\"y\":$y}"
        is LineTo -> "{\"type\":\"LINE\",\"x\":$x,\"y\":$y}"
        Clear -> "{\"type\":\"CLEAR\"}"
    }

    companion object {
        fun fromJson(jsonStr: String): CanvasEvent? = try {
            val obj = JSONObject(jsonStr)
            when (obj.optString("type")) {
                "MOVE" -> MoveTo(
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat()
                )
                "LINE" -> LineTo(
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat()
                )
                "CLEAR" -> Clear
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Synchronized Face-to-Face Video, Shared Flashcard, and P2P Whiteboard Canvas.
 *
 * REAL-TIME COLLABORATIVE ARCHITECTURE:
 * - Uses relative coordinate mathematics (0.0f .. 1.0f) so devices with different screen
 *   dimensions, aspect ratios, and resolutions render identical ink paths.
 * - Local strokes are drawn in [WarmCoral] and broadcast instantly via Jami Data Channel.
 * - Remote peer strokes are received over Jami Data Channel, multiplied by the local canvas
 *   width/height, and drawn in [PureWhite].
 * - Minimal allocation in drag gestures preserves 60/120 FPS high-refresh touch response.
 * - Synchronized Eraser tool resets both local and remote canvas paths across devices.
 * - Shared Flashcard Image Drop: Photos picked from the gallery are cached, transferred
 *   over the Jami Swarm, and rendered beneath the drawing canvas so users can annotate directly on them.
 */
@Composable
fun CanvasScreen(
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
    jamiDaemon: JamiDaemon = remember { JamiDaemonImpl() }
) {
    val context = LocalContext.current

    // References to native SurfaceViews for Jami video pipeline
    var localSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var remoteSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    // Canvas dimensions for relative coordinate normalization
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Shared Flashcard state (URI resolved to image)
    var sharedImageUri by remember { mutableStateOf<Uri?>(null) }
    var sharedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Photo picker launcher for Image Drop feature
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri ->
        if (selectedUri != null) {
            sharedImageUri = selectedUri
            // Resolve Android Content URI to local cache file for Jami transmission
            val cachedFile = copyUriToCacheFile(context, selectedUri)
            if (cachedFile != null) {
                jamiDaemon.sendFileToSwarm(cachedFile.absolutePath)
            }
        }
    }

    // Independent drawing paths for local (Warm Coral) and remote (Pure White) ink
    val localPath = remember { Path() }
    val remotePath = remember { Path() }
    var motionTick by remember { mutableLongStateOf(0L) }

    // Bind Android SurfaceViews to Jami video pipeline
    LaunchedEffect(localSurfaceView, remoteSurfaceView) {
        val local = localSurfaceView
        val remote = remoteSurfaceView
        if (local != null && remote != null) {
            jamiDaemon.bindVideoViews(localView = local, remoteView = remote)
        }
    }

    // Observe incoming remote files (e.g. shared flashcard images) from Jami Swarm
    LaunchedEffect(jamiDaemon) {
        jamiDaemon.incomingFiles.collect { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                sharedImageUri = Uri.fromFile(file)
            }
        }
    }

    // Decode shared image URI to ImageBitmap in background IO dispatcher
    LaunchedEffect(sharedImageUri) {
        val uri = sharedImageUri
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val stream = if (uri.scheme == "file") {
                        File(uri.path ?: "").inputStream()
                    } else {
                        context.contentResolver.openInputStream(uri)
                    }
                    stream?.use {
                        val decoded = BitmapFactory.decodeStream(it)
                        sharedImageBitmap = decoded?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    sharedImageBitmap = null
                }
            }
        } else {
            sharedImageBitmap = null
        }
    }

    // Observe incoming remote canvas strokes over Jami Data Channel
    LaunchedEffect(jamiDaemon, canvasSize) {
        jamiDaemon.incomingCanvasStrokes.collect { payload ->
            val event = CanvasEvent.fromJson(payload) ?: return@collect
            val w = canvasSize.width.toFloat()
            val h = canvasSize.height.toFloat()

            when (event) {
                is CanvasEvent.MoveTo -> {
                    if (w > 0f && h > 0f) {
                        remotePath.moveTo(event.x * w, event.y * h)
                        motionTick++
                    }
                }
                is CanvasEvent.LineTo -> {
                    if (w > 0f && h > 0f) {
                        remotePath.lineTo(event.x * w, event.y * h)
                        motionTick++
                    }
                }
                CanvasEvent.Clear -> {
                    localPath.reset()
                    remotePath.reset()
                    motionTick++
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNavy)
    ) {
        // Layer 1: Background Remote Video (Full-screen SurfaceView)
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).also { view ->
                    remoteSurfaceView = view
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Foreground PIP Local Camera (100dp circular SurfaceView in top-right corner)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 44.dp, end = 24.dp)
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, WarmCoral, CircleShape)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        setZOrderMediaOverlay(true)
                    }.also { view ->
                        localSurfaceView = view
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Layer 2.5: Shared Flashcard Image (Rendered beneath Canvas so ink appears over image)
        sharedImageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Shared Flashcard",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 96.dp)
            )
        }

        // Layer 3: Synchronized P2P Drawing Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(canvasSize) {
                    if (canvasSize.width <= 0 || canvasSize.height <= 0) return@pointerInput

                    val widthFloat = canvasSize.width.toFloat()
                    val heightFloat = canvasSize.height.toFloat()

                    detectDragGestures(
                        onDragStart = { offset ->
                            val relX = (offset.x / widthFloat).coerceIn(0f, 1f)
                            val relY = (offset.y / heightFloat).coerceIn(0f, 1f)

                            localPath.moveTo(offset.x, offset.y)
                            motionTick++

                            // Stream relative coordinate over Jami Data Channel
                            jamiDaemon.sendDataChannelPayload(
                                CanvasEvent.MoveTo(relX, relY).toJson()
                            )
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val relX = (change.position.x / widthFloat).coerceIn(0f, 1f)
                            val relY = (change.position.y / heightFloat).coerceIn(0f, 1f)

                            localPath.lineTo(change.position.x, change.position.y)
                            motionTick++

                            // Stream relative coordinate over Jami Data Channel
                            jamiDaemon.sendDataChannelPayload(
                                CanvasEvent.LineTo(relX, relY).toJson()
                            )
                        }
                    )
                }
        ) {
            motionTick.let { _ ->
                // Draw remote peer strokes in Pure White
                drawPath(
                    path = remotePath,
                    color = PureWhite,
                    style = Stroke(
                        width = 16f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Draw local user strokes in Warm Coral
                drawPath(
                    path = localPath,
                    color = WarmCoral,
                    style = Stroke(
                        width = 16f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Layer 4: Controls (Perfectly centered bottom action bar: Gallery, Eraser, End Call)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button: Photo Picker (Image Drop)
            FloatingActionButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                containerColor = ElevatedNavy,
                contentColor = WarmCoral,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Share Image Flashcard",
                    tint = WarmCoral,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Center Button: Synchronized Eraser Tool
            FloatingActionButton(
                onClick = {
                    localPath.reset()
                    remotePath.reset()
                    motionTick++
                    jamiDaemon.sendDataChannelPayload(CanvasEvent.Clear.toJson())
                },
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                containerColor = ElevatedNavy,
                contentColor = WarmCoral,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Canvas",
                    tint = WarmCoral,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Right Button: End Call
            FloatingActionButton(
                onClick = onEndCall,
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                containerColor = WarmCoral,
                contentColor = PureWhite,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = PureWhite,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

/**
 * Copies a Content/File URI into the application cache directory for P2P transmission.
 */
private fun copyUriToCacheFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val extension = context.contentResolver.getType(uri)?.substringAfterLast("/") ?: "jpg"
        val outputFile = File(
            context.cacheDir,
            "flashcard_${System.currentTimeMillis()}.$extension"
        )
        outputFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        outputFile
    } catch (_: Exception) {
        null
    }
}

package tech.isano.language.core.engine.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class VoiceNoteRecorderTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir", "/tmp"), "voicenote_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    private class FakeAudioRecorderEngine(
        val onStopThrows: Boolean = false
    ) : VoiceNoteRecorder.AudioRecorderEngine {
        var isPreparedAndStarted: Boolean = false
        var isStopped: Boolean = false
        var isReleased: Boolean = false

        override fun prepareAndStart() {
            isPreparedAndStarted = true
        }

        override fun stop() {
            if (onStopThrows) {
                throw RuntimeException("stop failed: rapid tap")
            }
            isStopped = true
        }

        override fun release() {
            isReleased = true
        }
    }

    @Test
    fun testVoiceNoteConstants() {
        assertEquals("voicenote_", VoiceNoteRecorder.FILE_PREFIX)
        assertEquals(".aac", VoiceNoteRecorder.FILE_EXTENSION)
        assertEquals(32_000, VoiceNoteRecorder.SPEECH_BITRATE)
        assertEquals(16_000, VoiceNoteRecorder.SPEECH_SAMPLE_RATE)
    }

    @Test
    fun testVoiceNoteFileNamingAndDirectory() {
        val fakeEngine = FakeAudioRecorderEngine()
        val recorder = VoiceNoteRecorder(
            targetDir = tempDir,
            engineFactory = { fakeEngine }
        )

        val outputFile = recorder.startRecording()

        assertNotNull(outputFile)
        assertTrue(outputFile.name.startsWith(VoiceNoteRecorder.FILE_PREFIX))
        assertTrue(outputFile.name.endsWith(VoiceNoteRecorder.FILE_EXTENSION))
        assertEquals(tempDir.absolutePath, outputFile.parentFile?.absolutePath)
        assertEquals(outputFile, recorder.getCurrentFile())
        assertTrue(recorder.isRecording)
        assertTrue(fakeEngine.isPreparedAndStarted)

        recorder.stopRecording()
        assertFalse(recorder.isRecording)
        assertTrue(fakeEngine.isStopped)
        assertTrue(fakeEngine.isReleased)
    }

    @Test
    fun testConsecutiveStartRecording_cleansUpPreviousRecording() {
        val engines = mutableListOf<FakeAudioRecorderEngine>()
        val recorder = VoiceNoteRecorder(
            targetDir = tempDir,
            engineFactory = {
                val engine = FakeAudioRecorderEngine()
                engines.add(engine)
                engine
            }
        )

        val firstFile = recorder.startRecording()
        assertEquals(1, engines.size)
        assertTrue(recorder.isRecording)
        assertTrue(engines[0].isPreparedAndStarted)
        assertFalse(engines[0].isStopped)

        val secondFile = recorder.startRecording()
        assertEquals(2, engines.size)
        assertTrue(recorder.isRecording)
        // First engine was stopped and released
        assertTrue(engines[0].isStopped)
        assertTrue(engines[0].isReleased)
        // Second engine was started
        assertTrue(engines[1].isPreparedAndStarted)
        assertEquals(secondFile, recorder.getCurrentFile())

        recorder.stopRecording()
        assertFalse(recorder.isRecording)
        assertTrue(engines[1].isStopped)
        assertTrue(engines[1].isReleased)
    }

    @Test
    fun testRapidStopRecordingExceptionHandledGracefully() {
        val fakeEngine = FakeAudioRecorderEngine(onStopThrows = true)
        val recorder = VoiceNoteRecorder(
            targetDir = tempDir,
            engineFactory = { fakeEngine }
        )

        recorder.startRecording()
        assertTrue(recorder.isRecording)

        // Must not crash even if engine throws during stop()
        recorder.stopRecording()
        assertFalse(recorder.isRecording)
        assertTrue(fakeEngine.isReleased)
    }
}

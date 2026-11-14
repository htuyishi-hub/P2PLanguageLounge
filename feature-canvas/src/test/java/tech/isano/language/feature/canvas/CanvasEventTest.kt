package tech.isano.language.feature.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.isano.language.feature.canvas.ui.CanvasEvent

class CanvasEventTest {

    @Test
    fun testMoveToSerializationAndDeserialization() {
        val event = CanvasEvent.MoveTo(0.25f, 0.75f)
        val json = event.toJson()

        val parsed = CanvasEvent.fromJson(json)
        assertNotNull(parsed)
        assertTrue(parsed is CanvasEvent.MoveTo)
        val moveTo = parsed as CanvasEvent.MoveTo
        assertEquals(0.25f, moveTo.x, 0.001f)
        assertEquals(0.75f, moveTo.y, 0.001f)
    }

    @Test
    fun testLineToSerializationAndDeserialization() {
        val event = CanvasEvent.LineTo(0.5f, 0.9f)
        val json = event.toJson()

        val parsed = CanvasEvent.fromJson(json)
        assertNotNull(parsed)
        assertTrue(parsed is CanvasEvent.LineTo)
        val lineTo = parsed as CanvasEvent.LineTo
        assertEquals(0.5f, lineTo.x, 0.001f)
        assertEquals(0.9f, lineTo.y, 0.001f)
    }

    @Test
    fun testClearSerializationAndDeserialization() {
        val event = CanvasEvent.Clear
        val json = event.toJson()

        val parsed = CanvasEvent.fromJson(json)
        assertNotNull(parsed)
        assertEquals(CanvasEvent.Clear, parsed)
    }

    @Test
    fun testMalformedJsonReturnsNullSafely() {
        assertNull(CanvasEvent.fromJson("not-a-json"))
        assertNull(CanvasEvent.fromJson("{\"type\":\"UNKNOWN\"}"))
        assertNull(CanvasEvent.fromJson(""))
    }
}

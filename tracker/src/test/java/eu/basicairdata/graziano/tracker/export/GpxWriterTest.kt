package eu.basicairdata.graziano.tracker.export

import eu.basicairdata.graziano.tracker.data.LoggedPoint
import eu.basicairdata.graziano.tracker.data.TrackingSession
import eu.basicairdata.graziano.tracker.data.Waypoint
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringWriter

class GpxWriterTest {

    @Test
    fun writeSession_containsGpx11HeaderAndTrackPoints() {
        val session = TrackingSession(
            id = 1, name = "Track Test", startTimeMillis = 0, intervalMinutes = 15
        )
        val points = listOf(
            LoggedPoint(sessionId = 1, timestampMillis = 0L, latitude = 45.5, longitude = 9.25, altitudeMeters = 120.0),
            LoggedPoint(sessionId = 1, timestampMillis = 60_000L, latitude = 45.51, longitude = 9.26)
        )
        val out = StringWriter()

        GpxWriter.writeSession(session, points, out)
        val xml = out.toString()

        assertTrue(xml.contains("version=\"1.1\""))
        assertTrue(xml.contains("xmlns=\"http://www.topografix.com/GPX/1/1\""))
        assertTrue(xml.contains("<name>Track Test</name>"))
        assertTrue(xml.contains("<trkpt lat=\"45.50000000\" lon=\"9.25000000\">"))
        assertTrue(xml.contains("<ele>120.00</ele>"))
        assertTrue(xml.contains("<trkpt lat=\"45.51000000\" lon=\"9.26000000\">"))
        assertTrue(xml.trim().endsWith("</gpx>"))
    }

    @Test
    fun writeWaypoints_escapesNameAndWritesEachWpt() {
        val waypoints = listOf(
            Waypoint(id = 1, name = "A & B", timestampMillis = 0L, latitude = 1.0, longitude = 2.0)
        )
        val out = StringWriter()

        GpxWriter.writeWaypoints(waypoints, out)
        val xml = out.toString()

        assertTrue(xml.contains("<wpt lat=\"1.00000000\" lon=\"2.00000000\">"))
        assertTrue(xml.contains("<name>A &amp; B</name>"))
    }
}

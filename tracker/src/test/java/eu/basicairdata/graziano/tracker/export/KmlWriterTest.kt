package eu.basicairdata.graziano.tracker.export

import eu.basicairdata.graziano.tracker.data.LoggedPoint
import eu.basicairdata.graziano.tracker.data.TrackingSession
import eu.basicairdata.graziano.tracker.data.Waypoint
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringWriter

class KmlWriterTest {

    @Test
    fun writeSession_containsKml22NamespaceAndLineString() {
        val session = TrackingSession(id = 1, name = "Track Test", startTimeMillis = 0, intervalMinutes = 15)
        val points = listOf(
            LoggedPoint(sessionId = 1, timestampMillis = 0L, latitude = 45.5, longitude = 9.25, altitudeMeters = 120.0)
        )
        val out = StringWriter()

        KmlWriter.writeSession(session, points, out)
        val xml = out.toString()

        assertTrue(xml.contains("xmlns=\"http://www.opengis.net/kml/2.2\""))
        assertTrue(xml.contains("<LineString>"))
        assertTrue(xml.contains("9.25000000,45.50000000,120.00"))
        assertTrue(xml.trim().endsWith("</kml>"))
    }

    @Test
    fun writeWaypoints_writesPointPlacemarkPerWaypoint() {
        val waypoints = listOf(
            Waypoint(id = 1, name = "Home", timestampMillis = 0L, latitude = 1.0, longitude = 2.0)
        )
        val out = StringWriter()

        KmlWriter.writeWaypoints(waypoints, out)
        val xml = out.toString()

        assertTrue(xml.contains("<name>Home</name>"))
        assertTrue(xml.contains("<Point>"))
        assertTrue(xml.contains("2.00000000,1.00000000,0.00"))
    }
}

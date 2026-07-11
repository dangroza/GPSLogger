package eu.basicairdata.graziano.tracker.export

import eu.basicairdata.graziano.tracker.data.Waypoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringWriter

class CsvWriterTest {

    @Test
    fun writeWaypoints_headerAndEscapedRow() {
        val waypoints = listOf(
            Waypoint(
                id = 1,
                name = "Home, sweet home",
                timestampMillis = 0L,
                latitude = 45.5,
                longitude = 9.25,
                accuracyMeters = 5f,
                altitudeMeters = 120.0
            )
        )
        val out = StringWriter()

        CsvWriter.writeWaypoints(waypoints, out)
        val lines = out.toString().trim().lines()

        assertEquals("name,timestamp_utc,latitude,longitude,accuracy_m,altitude_m", lines[0])
        assertEquals(
            "\"Home, sweet home\",1970-01-01 00:00:00,45.50000000,9.25000000,5.00,120.00",
            lines[1]
        )
    }
}

package eu.basicairdata.graziano.tracker.export

import eu.basicairdata.graziano.tracker.data.LoggedPoint
import eu.basicairdata.graziano.tracker.data.TrackingSession
import eu.basicairdata.graziano.tracker.data.Waypoint
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Writes standard GPX 1.1 (no proprietary extensions), so exports stay interoperable
 * with any external tool — including a future track-management screen.
 */
object GpxWriter {

    private const val CREATOR = "GPS Tracker Lite"

    private fun timeFormatter(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }

    private fun header(out: Writer) {
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        out.write(
            "<gpx version=\"1.1\" creator=\"$CREATOR\" " +
                "xmlns=\"http://www.topografix.com/GPX/1/1\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 " +
                "http://www.topografix.com/GPX/1/1/gpx.xsd\">\n"
        )
    }

    private fun footer(out: Writer) {
        out.write("</gpx>\n")
    }

    fun writeSession(session: TrackingSession, points: List<LoggedPoint>, out: Writer) {
        val df = timeFormatter()
        header(out)
        out.write("  <trk>\n")
        out.write("    <name>${session.name.xmlEscape()}</name>\n")
        out.write("    <trkseg>\n")
        for (point in points) {
            out.write(
                "      <trkpt lat=\"${"%.8f".format(Locale.US, point.latitude)}\" " +
                    "lon=\"${"%.8f".format(Locale.US, point.longitude)}\">\n"
            )
            point.altitudeMeters?.let {
                out.write("        <ele>${"%.2f".format(Locale.US, it)}</ele>\n")
            }
            out.write("        <time>${df.format(Date(point.timestampMillis))}</time>\n")
            out.write("      </trkpt>\n")
        }
        out.write("    </trkseg>\n")
        out.write("  </trk>\n")
        footer(out)
    }

    fun writeWaypoints(waypoints: List<Waypoint>, out: Writer) {
        val df = timeFormatter()
        header(out)
        for (waypoint in waypoints) {
            out.write(
                "  <wpt lat=\"${"%.8f".format(Locale.US, waypoint.latitude)}\" " +
                    "lon=\"${"%.8f".format(Locale.US, waypoint.longitude)}\">\n"
            )
            waypoint.altitudeMeters?.let {
                out.write("    <ele>${"%.2f".format(Locale.US, it)}</ele>\n")
            }
            out.write("    <time>${df.format(Date(waypoint.timestampMillis))}</time>\n")
            out.write("    <name>${waypoint.name.xmlEscape()}</name>\n")
            out.write("  </wpt>\n")
        }
        footer(out)
    }
}

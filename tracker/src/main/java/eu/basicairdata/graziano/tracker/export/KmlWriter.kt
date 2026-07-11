package eu.basicairdata.graziano.tracker.export

import eu.basicairdata.graziano.tracker.data.LoggedPoint
import eu.basicairdata.graziano.tracker.data.TrackingSession
import eu.basicairdata.graziano.tracker.data.Waypoint
import java.io.Writer
import java.util.Locale

/**
 * Writes standard KML 2.2 (no proprietary extensions), so exports stay interoperable
 * with any external tool — including a future track-management screen.
 */
object KmlWriter {

    private fun header(out: Writer, documentName: String) {
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        out.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
        out.write("  <Document>\n")
        out.write("    <name>${documentName.xmlEscape()}</name>\n")
        out.write("    <Style id=\"TrackLineStyle\">\n")
        out.write("      <LineStyle><color>ff0000ff</color><width>4</width></LineStyle>\n")
        out.write("    </Style>\n")
        out.write("    <Style id=\"PlacemarkStyle\">\n")
        out.write(
            "      <IconStyle><Icon><href>" +
                "http://maps.google.com/mapfiles/kml/shapes/placemark_circle_highlight.png" +
                "</href></Icon></IconStyle>\n"
        )
        out.write("    </Style>\n")
    }

    private fun footer(out: Writer) {
        out.write("  </Document>\n")
        out.write("</kml>\n")
    }

    private fun coordinates(lat: Double, lon: Double, alt: Double?): String =
        "${"%.8f".format(Locale.US, lon)},${"%.8f".format(Locale.US, lat)},${"%.2f".format(Locale.US, alt ?: 0.0)}"

    fun writeSession(session: TrackingSession, points: List<LoggedPoint>, out: Writer) {
        header(out, session.name)
        out.write("    <Placemark>\n")
        out.write("      <name>${session.name.xmlEscape()}</name>\n")
        out.write("      <styleUrl>#TrackLineStyle</styleUrl>\n")
        out.write("      <LineString>\n")
        out.write("        <tessellate>1</tessellate>\n")
        out.write("        <coordinates>\n")
        for (point in points) {
            out.write("          ${coordinates(point.latitude, point.longitude, point.altitudeMeters)}\n")
        }
        out.write("        </coordinates>\n")
        out.write("      </LineString>\n")
        out.write("    </Placemark>\n")
        footer(out)
    }

    fun writeWaypoints(waypoints: List<Waypoint>, out: Writer) {
        header(out, "Waypoints")
        for (waypoint in waypoints) {
            out.write("    <Placemark>\n")
            out.write("      <name>${waypoint.name.xmlEscape()}</name>\n")
            out.write("      <styleUrl>#PlacemarkStyle</styleUrl>\n")
            out.write("      <Point>\n")
            out.write(
                "        <coordinates>${coordinates(waypoint.latitude, waypoint.longitude, waypoint.altitudeMeters)}</coordinates>\n"
            )
            out.write("      </Point>\n")
            out.write("    </Placemark>\n")
        }
        footer(out)
    }
}

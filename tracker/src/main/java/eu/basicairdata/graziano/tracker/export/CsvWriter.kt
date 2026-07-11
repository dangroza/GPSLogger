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
 * Plain CSV with a standard header row — importable as-is into Google My Maps
 * (Import layer -> pick latitude/longitude columns).
 */
object CsvWriter {

    private fun timeFormatter(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }

    private fun String.csvEscape(): String =
        if (contains(',') || contains('"') || contains('\n')) {
            "\"${replace("\"", "\"\"")}\""
        } else this

    fun writeWaypoints(waypoints: List<Waypoint>, out: Writer) {
        val df = timeFormatter()
        out.write("name,timestamp_utc,latitude,longitude,accuracy_m,altitude_m\n")
        for (waypoint in waypoints) {
            out.write(
                listOf(
                    waypoint.name.csvEscape(),
                    df.format(Date(waypoint.timestampMillis)),
                    "%.8f".format(Locale.US, waypoint.latitude),
                    "%.8f".format(Locale.US, waypoint.longitude),
                    waypoint.accuracyMeters?.let { "%.2f".format(Locale.US, it) } ?: "",
                    waypoint.altitudeMeters?.let { "%.2f".format(Locale.US, it) } ?: ""
                ).joinToString(",") + "\n"
            )
        }
    }

    fun writeSession(session: TrackingSession, points: List<LoggedPoint>, out: Writer) {
        val df = timeFormatter()
        out.write("name,timestamp_utc,latitude,longitude,accuracy_m,altitude_m\n")
        for (point in points) {
            out.write(
                listOf(
                    session.name.csvEscape(),
                    df.format(Date(point.timestampMillis)),
                    "%.8f".format(Locale.US, point.latitude),
                    "%.8f".format(Locale.US, point.longitude),
                    point.accuracyMeters?.let { "%.2f".format(Locale.US, it) } ?: "",
                    point.altitudeMeters?.let { "%.2f".format(Locale.US, it) } ?: ""
                ).joinToString(",") + "\n"
            )
        }
    }
}

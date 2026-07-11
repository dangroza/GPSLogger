package eu.basicairdata.graziano.tracker.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import eu.basicairdata.graziano.tracker.data.LoggedPoint
import eu.basicairdata.graziano.tracker.data.TrackingSession
import eu.basicairdata.graziano.tracker.data.Waypoint
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

private const val FILE_PROVIDER_AUTHORITY = "eu.basicairdata.graziano.tracker.fileprovider"

class ExportManager(private val context: Context) {

    private fun exportsDir(): File =
        File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }

    private fun sanitize(name: String): String = name.replace(Regex("[^A-Za-z0-9_-]"), "_")

    fun exportSession(session: TrackingSession, points: List<LoggedPoint>, format: ExportFormat): Uri {
        val file = File(exportsDir(), "${sanitize(session.name)}.${format.extension}")
        BufferedWriter(FileWriter(file)).use { out ->
            when (format) {
                ExportFormat.GPX -> GpxWriter.writeSession(session, points, out)
                ExportFormat.KML -> KmlWriter.writeSession(session, points, out)
                ExportFormat.CSV -> CsvWriter.writeSession(session, points, out)
            }
        }
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
    }

    fun exportAllWaypoints(waypoints: List<Waypoint>, format: ExportFormat): Uri {
        val file = File(exportsDir(), "waypoints.${format.extension}")
        BufferedWriter(FileWriter(file)).use { out ->
            when (format) {
                ExportFormat.GPX -> GpxWriter.writeWaypoints(waypoints, out)
                ExportFormat.KML -> KmlWriter.writeWaypoints(waypoints, out)
                ExportFormat.CSV -> CsvWriter.writeWaypoints(waypoints, out)
            }
        }
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
    }

    fun shareFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

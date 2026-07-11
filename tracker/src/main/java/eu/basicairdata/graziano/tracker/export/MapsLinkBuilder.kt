package eu.basicairdata.graziano.tracker.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

/** Builds/shares a Google Maps link for a single point, mirroring Android's native "Share location". */
object MapsLinkBuilder {

    fun webUrl(lat: Double, lon: Double): String =
        "https://www.google.com/maps?q=${"%.8f".format(Locale.US, lat)},${"%.8f".format(Locale.US, lon)}"

    fun geoUri(lat: Double, lon: Double, label: String): Uri =
        Uri.parse(
            "geo:${"%.8f".format(Locale.US, lat)},${"%.8f".format(Locale.US, lon)}" +
                "?q=${"%.8f".format(Locale.US, lat)},${"%.8f".format(Locale.US, lon)}(${Uri.encode(label)})"
        )

    fun shareIntent(lat: Double, lon: Double, label: String): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$label: ${webUrl(lat, lon)}")
            },
            null
        )

    fun openInMapsIntent(lat: Double, lon: Double, label: String): Intent =
        Intent(Intent.ACTION_VIEW, geoUri(lat, lon, label))

    fun share(context: Context, lat: Double, lon: Double, label: String) {
        context.startActivity(shareIntent(lat, lon, label))
    }
}

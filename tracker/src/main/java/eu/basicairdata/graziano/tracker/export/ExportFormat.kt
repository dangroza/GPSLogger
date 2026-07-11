package eu.basicairdata.graziano.tracker.export

enum class ExportFormat(val extension: String, val mimeType: String) {
    GPX("gpx", "application/gpx+xml"),
    KML("kml", "application/vnd.google-earth.kml+xml"),
    CSV("csv", "text/csv")
}

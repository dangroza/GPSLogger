package eu.basicairdata.graziano.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A point of interest saved via "Quick Save". Independent of any [TrackingSession] —
 * if a session is active when saved, the same fix is also written as a [LoggedPoint]
 * for that session (see MainViewModel.quickSave), so it appears both here and in the track.
 */
@Entity(tableName = "waypoints")
data class Waypoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null
)

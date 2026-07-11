package eu.basicairdata.graziano.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class TrackingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val intervalMinutes: Int,
    val isActive: Boolean = true
)

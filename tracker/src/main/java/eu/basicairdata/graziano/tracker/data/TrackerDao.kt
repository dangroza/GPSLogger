package eu.basicairdata.graziano.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {

    @Insert
    suspend fun insertSession(session: TrackingSession): Long

    @Update
    suspend fun updateSession(session: TrackingSession)

    @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): TrackingSession?

    @Query("SELECT * FROM sessions ORDER BY startTimeMillis DESC")
    fun observeSessions(): Flow<List<TrackingSession>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): TrackingSession?

    @Insert
    suspend fun insertPoint(point: LoggedPoint): Long

    @Query("SELECT * FROM points WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    suspend fun getPointsForSession(sessionId: Long): List<LoggedPoint>

    @Query("SELECT COUNT(*) FROM points WHERE sessionId = :sessionId")
    fun observePointCount(sessionId: Long): Flow<Int>

    @Insert
    suspend fun insertWaypoint(waypoint: Waypoint): Long

    @Query("SELECT * FROM waypoints ORDER BY timestampMillis DESC")
    fun observeWaypoints(): Flow<List<Waypoint>>

    @Query("SELECT * FROM waypoints")
    suspend fun getAllWaypoints(): List<Waypoint>

    @Delete
    suspend fun deleteWaypoint(waypoint: Waypoint)
}

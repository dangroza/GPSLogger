package eu.basicairdata.graziano.tracker.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackerDaoTest {

    private lateinit var db: TrackerDatabase
    private lateinit var dao: TrackerDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TrackerDatabase::class.java).build()
        dao = db.trackerDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertSession_thenGetActiveSession_returnsIt() = runBlocking {
        val id = dao.insertSession(
            TrackingSession(name = "Track A", startTimeMillis = 0L, intervalMinutes = 15, isActive = true)
        )

        val active = dao.getActiveSession()

        assertEquals(id, active?.id)
        assertEquals("Track A", active?.name)
    }

    @Test
    fun insertPoints_forSession_returnedInTimestampOrder() = runBlocking {
        val sessionId = dao.insertSession(
            TrackingSession(name = "Track A", startTimeMillis = 0L, intervalMinutes = 15)
        )
        dao.insertPoint(LoggedPoint(sessionId = sessionId, timestampMillis = 2000L, latitude = 1.0, longitude = 1.0))
        dao.insertPoint(LoggedPoint(sessionId = sessionId, timestampMillis = 1000L, latitude = 2.0, longitude = 2.0))

        val points = dao.getPointsForSession(sessionId)

        assertEquals(2, points.size)
        assertEquals(1000L, points[0].timestampMillis)
        assertEquals(2000L, points[1].timestampMillis)
    }

    @Test
    fun deletingSession_cascadesToItsPoints() = runBlocking {
        val sessionId = dao.insertSession(
            TrackingSession(name = "Track A", startTimeMillis = 0L, intervalMinutes = 15)
        )
        dao.insertPoint(LoggedPoint(sessionId = sessionId, timestampMillis = 1000L, latitude = 1.0, longitude = 1.0))

        db.openHelper.writableDatabase.delete("sessions", "id = ?", arrayOf(sessionId.toString()))

        assertTrue(dao.getPointsForSession(sessionId).isEmpty())
    }

    @Test
    fun insertWaypoint_isIndependentOfSessions() = runBlocking {
        dao.insertWaypoint(
            Waypoint(name = "POI", timestampMillis = 0L, latitude = 3.0, longitude = 4.0)
        )

        val all = dao.getAllWaypoints()

        assertEquals(1, all.size)
        assertEquals("POI", all[0].name)
        assertNull(dao.getActiveSession())
    }
}

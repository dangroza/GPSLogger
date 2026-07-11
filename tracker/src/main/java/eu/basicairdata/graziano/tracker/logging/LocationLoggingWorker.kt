package eu.basicairdata.graziano.tracker.logging

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import eu.basicairdata.graziano.tracker.data.LoggedPoint
import eu.basicairdata.graziano.tracker.data.TrackerDatabase
import eu.basicairdata.graziano.tracker.util.PermissionUtils
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs on every WorkManager periodic tick (minimum 15 min apart). Requests a single
 * low-power location fix and goes back to sleep — no continuous listener, no wakelock
 * beyond what WorkManager already holds for the duration of [doWork].
 */
class LocationLoggingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = TrackerDatabase.get(applicationContext).trackerDao()
        val session = dao.getActiveSession() ?: return Result.success()

        if (!PermissionUtils.hasBackgroundLocationPermission(applicationContext)) {
            return Result.failure()
        }

        val client = LocationServices.getFusedLocationProviderClient(applicationContext)
        val location = try {
            withTimeoutOrNull(FIX_TIMEOUT_MILLIS) {
                client.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token
                ).await()
            }
        } catch (e: SecurityException) {
            null
        }

        if (location == null) return Result.retry()

        dao.insertPoint(
            LoggedPoint(
                sessionId = session.id,
                timestampMillis = location.time,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                altitudeMeters = if (location.hasAltitude()) location.altitude else null
            )
        )
        return Result.success()
    }

    companion object {
        private const val FIX_TIMEOUT_MILLIS = 45_000L
    }
}

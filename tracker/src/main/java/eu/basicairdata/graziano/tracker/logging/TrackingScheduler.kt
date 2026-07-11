package eu.basicairdata.graziano.tracker.logging

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/** Minimum interval WorkManager allows for periodic work. */
const val MIN_INTERVAL_MINUTES = 15L

object TrackingScheduler {

    private const val UNIQUE_WORK_NAME = "location_logging_periodic"

    fun start(context: Context, intervalMinutes: Long) {
        val request = PeriodicWorkRequestBuilder<LocationLoggingWorker>(
            intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES), TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun isScheduled(context: Context): LiveData<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME)
            .map { infos -> infos.any { !it.state.isFinished } }
}

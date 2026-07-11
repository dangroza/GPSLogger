package eu.basicairdata.graziano.tracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import eu.basicairdata.graziano.tracker.data.LoggedPoint
import eu.basicairdata.graziano.tracker.data.TrackerDatabase
import eu.basicairdata.graziano.tracker.data.TrackingSession
import eu.basicairdata.graziano.tracker.data.Waypoint
import eu.basicairdata.graziano.tracker.export.ExportFormat
import eu.basicairdata.graziano.tracker.export.ExportManager
import eu.basicairdata.graziano.tracker.logging.MIN_INTERVAL_MINUTES
import eu.basicairdata.graziano.tracker.logging.TrackingScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class QuickSaveResult {
    data class Success(val waypoint: Waypoint) : QuickSaveResult()
    object Failure : QuickSaveResult()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = TrackerDatabase.get(application).trackerDao()
    private val exportManager = ExportManager(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val nameFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    val sessions: StateFlow<List<TrackingSession>> =
        dao.observeSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val waypoints: StateFlow<List<Waypoint>> =
        dao.observeWaypoints().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _activeSession = MutableStateFlow<TrackingSession?>(null)
    val activeSession: StateFlow<TrackingSession?> = _activeSession

    init {
        viewModelScope.launch { _activeSession.value = dao.getActiveSession() }
    }

    fun startTracking(intervalMinutes: Long) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = TrackingSession(
                name = "Track ${nameFormatter.format(Date(now))}",
                startTimeMillis = now,
                intervalMinutes = intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES).toInt(),
                isActive = true
            )
            val id = dao.insertSession(session)
            _activeSession.value = session.copy(id = id)
            TrackingScheduler.start(getApplication(), intervalMinutes)
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            val session = _activeSession.value ?: return@launch
            dao.updateSession(session.copy(isActive = false, endTimeMillis = System.currentTimeMillis()))
            TrackingScheduler.stop(getApplication())
            _activeSession.value = null
        }
    }

    /**
     * Immediate, foreground, one-shot fix — not routed through WorkManager, since the user
     * is actively waiting on screen. Always saved as a [Waypoint]; if a tracking session is
     * currently active, the same fix is also appended to that session's track as a [LoggedPoint].
     */
    fun quickSave(label: String? = null, onResult: (QuickSaveResult) -> Unit) {
        viewModelScope.launch {
            val location = try {
                withTimeoutOrNull(QUICK_SAVE_TIMEOUT_MILLIS) {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).await()
                } ?: fusedLocationClient.lastLocation.await()
            } catch (e: SecurityException) {
                null
            }

            if (location == null) {
                onResult(QuickSaveResult.Failure)
                return@launch
            }

            val now = System.currentTimeMillis()
            val name = label?.takeIf { it.isNotBlank() } ?: "Waypoint ${nameFormatter.format(Date(now))}"
            val waypoint = Waypoint(
                name = name,
                timestampMillis = now,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                altitudeMeters = if (location.hasAltitude()) location.altitude else null
            )
            val id = dao.insertWaypoint(waypoint)

            _activeSession.value?.let { session ->
                dao.insertPoint(
                    LoggedPoint(
                        sessionId = session.id,
                        timestampMillis = now,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                        altitudeMeters = if (location.hasAltitude()) location.altitude else null
                    )
                )
            }

            onResult(QuickSaveResult.Success(waypoint.copy(id = id)))
        }
    }

    fun deleteWaypoint(waypoint: Waypoint) {
        viewModelScope.launch { dao.deleteWaypoint(waypoint) }
    }

    fun exportSession(session: TrackingSession, format: ExportFormat, onExported: (Uri) -> Unit) {
        viewModelScope.launch {
            val points = dao.getPointsForSession(session.id)
            onExported(exportManager.exportSession(session, points, format))
        }
    }

    fun exportAllWaypoints(format: ExportFormat, onExported: (Uri) -> Unit) {
        viewModelScope.launch {
            val all = dao.getAllWaypoints()
            onExported(exportManager.exportAllWaypoints(all, format))
        }
    }

    fun shareExportedFile(uri: Uri, format: ExportFormat) {
        exportManager.shareFile(uri, format.mimeType)
    }

    companion object {
        private const val QUICK_SAVE_TIMEOUT_MILLIS = 10_000L
    }
}

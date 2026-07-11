package eu.basicairdata.graziano.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.basicairdata.graziano.tracker.R
import eu.basicairdata.graziano.tracker.data.TrackingSession
import eu.basicairdata.graziano.tracker.export.ExportFormat
import eu.basicairdata.graziano.tracker.logging.BatteryOptimizationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val INTERVAL_OPTIONS_MINUTES = listOf(15L, 30L, 60L, 120L, 240L)

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val permissions = rememberLocationPermissionsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val waypoints by viewModel.waypoints.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    var selectedInterval by remember { mutableStateOf(INTERVAL_OPTIONS_MINUTES.first()) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_tracking)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_waypoints)) }
                )
            }

            if (selectedTab == 0) {
                TrackingTab(
                    activeSession = activeSession,
                    sessions = sessions,
                    selectedInterval = selectedInterval,
                    onIntervalSelected = { selectedInterval = it },
                    hasForegroundPermission = permissions.hasForeground,
                    hasBackgroundPermission = permissions.hasBackground,
                    onRequestForeground = permissions.requestForeground,
                    onRequestBackground = permissions.requestBackground,
                    onStart = { viewModel.startTracking(selectedInterval) },
                    onStop = { viewModel.stopTracking() },
                    onQuickSave = {
                        viewModel.quickSave { result ->
                            scope.launch {
                                val message = when (result) {
                                    is QuickSaveResult.Success -> "Saved: ${result.waypoint.name}"
                                    QuickSaveResult.Failure -> "Could not get a location fix"
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    },
                    onExportSession = { session, format ->
                        viewModel.exportSession(session, format) { uri ->
                            viewModel.shareExportedFile(uri, format)
                        }
                    }
                )
            } else {
                WaypointsScreen(
                    waypoints = waypoints,
                    onShare = { waypoint ->
                        eu.basicairdata.graziano.tracker.export.MapsLinkBuilder.share(
                            context, waypoint.latitude, waypoint.longitude, waypoint.name
                        )
                    },
                    onDelete = { viewModel.deleteWaypoint(it) },
                    onExportAll = { format ->
                        viewModel.exportAllWaypoints(format) { uri ->
                            viewModel.shareExportedFile(uri, format)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackingTab(
    activeSession: TrackingSession?,
    sessions: List<TrackingSession>,
    selectedInterval: Long,
    onIntervalSelected: (Long) -> Unit,
    hasForegroundPermission: Boolean,
    hasBackgroundPermission: Boolean,
    onRequestForeground: () -> Unit,
    onRequestBackground: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onQuickSave: () -> Unit,
    onExportSession: (TrackingSession, ExportFormat) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (activeSession != null) {
                "Logging every ${activeSession.intervalMinutes} min — started ${dateFormat.format(Date(activeSession.startTimeMillis))}"
            } else {
                stringResource(R.string.status_idle)
            },
            style = MaterialTheme.typography.titleMedium
        )

        if (!hasForegroundPermission) {
            Text(stringResource(R.string.permission_foreground_rationale))
            Button(onClick = onRequestForeground) { Text("Grant location permission") }
        } else if (!hasBackgroundPermission) {
            Text(stringResource(R.string.permission_background_rationale))
            Button(onClick = onRequestBackground) { Text("Grant background location") }
        }

        IntervalPicker(
            selectedMinutes = selectedInterval,
            enabled = activeSession == null,
            onSelected = onIntervalSelected
        )
        Text(stringResource(R.string.interval_help), style = MaterialTheme.typography.bodySmall)

        Button(
            onClick = if (activeSession == null) onStart else onStop,
            enabled = hasForegroundPermission && hasBackgroundPermission
        ) {
            Text(stringResource(if (activeSession == null) R.string.action_start else R.string.action_stop))
        }

        Button(onClick = onQuickSave, enabled = hasForegroundPermission) {
            Text(stringResource(R.string.action_quick_save))
        }

        if (activeSession != null && !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.battery_optimization_banner))
                    Button(onClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) }) {
                        Text("Allow")
                    }
                }
            }
        }

        Text("Past sessions", style = MaterialTheme.typography.titleSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(sessions) { session ->
                SessionRow(session = session, onExport = { format -> onExportSession(session, format) })
            }
        }
    }
}

@Composable
private fun SessionRow(session: TrackingSession, onExport: (ExportFormat) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(session.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (session.isActive) "Active" else "Ended",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = { menuExpanded = true }) {
                Text(stringResource(R.string.action_export))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                ExportFormat.values().forEach { format ->
                    DropdownMenuItem(
                        text = { Text(format.name) },
                        onClick = {
                            menuExpanded = false
                            onExport(format)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalPicker(selectedMinutes: Long, enabled: Boolean, onSelected: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            readOnly = true,
            enabled = enabled,
            value = "$selectedMinutes min",
            onValueChange = {},
            label = { Text(stringResource(R.string.interval_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            INTERVAL_OPTIONS_MINUTES.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text("$minutes min") },
                    onClick = {
                        onSelected(minutes)
                        expanded = false
                    }
                )
            }
        }
    }
}

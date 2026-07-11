package eu.basicairdata.graziano.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.basicairdata.graziano.tracker.R
import eu.basicairdata.graziano.tracker.data.Waypoint
import eu.basicairdata.graziano.tracker.export.ExportFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WaypointsScreen(
    waypoints: List<Waypoint>,
    onShare: (Waypoint) -> Unit,
    onDelete: (Waypoint) -> Unit,
    onExportAll: (ExportFormat) -> Unit
) {
    var exportMenuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            TextButton(onClick = { exportMenuExpanded = true }) {
                Text(stringResource(R.string.action_export_all))
            }
            DropdownMenu(expanded = exportMenuExpanded, onDismissRequest = { exportMenuExpanded = false }) {
                ExportFormat.values().forEach { format ->
                    DropdownMenuItem(
                        text = { Text(format.name) },
                        onClick = {
                            exportMenuExpanded = false
                            onExportAll(format)
                        }
                    )
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(waypoints) { waypoint ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(waypoint.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${dateFormat.format(Date(waypoint.timestampMillis))} — " +
                                "${"%.6f".format(waypoint.latitude)}, ${"%.6f".format(waypoint.longitude)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row {
                            TextButton(onClick = { onShare(waypoint) }) {
                                Text(stringResource(R.string.action_share))
                            }
                            TextButton(onClick = { onDelete(waypoint) }) {
                                Text(stringResource(R.string.action_delete))
                            }
                        }
                    }
                }
            }
        }
    }
}

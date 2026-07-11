package eu.basicairdata.graziano.tracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.basicairdata.graziano.tracker.util.PermissionUtils

class LocationPermissionsState(
    val hasForeground: Boolean,
    val hasBackground: Boolean,
    val requestForeground: () -> Unit,
    val requestBackground: () -> Unit
)

@Composable
fun rememberLocationPermissionsState(): LocationPermissionsState {
    val context = LocalContext.current
    var hasForeground by remember { mutableStateOf(PermissionUtils.hasForegroundLocationPermission(context)) }
    var hasBackground by remember { mutableStateOf(PermissionUtils.hasBackgroundLocationPermission(context)) }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasForeground = result.values.any { it }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackground = granted
    }

    return LocationPermissionsState(
        hasForeground = hasForeground,
        hasBackground = hasBackground,
        requestForeground = {
            foregroundLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        },
        requestBackground = {
            // Android requires background location to be requested as its own, separate
            // prompt after foreground is already granted (Android 11+ policy).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                hasBackground = true
            }
        }
    )
}

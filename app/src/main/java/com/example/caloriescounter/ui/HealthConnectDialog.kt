package com.example.caloriescounter.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.caloriescounter.data.HealthConnectManager

@Composable
internal fun HealthConnectDialog(
    manager: HealthConnectManager,
    inTutorial: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val available = remember(manager, refresh) { manager.isSdkAvailable() }
    var granted by remember { mutableStateOf<Set<String>?>(null) }
    val missing = granted?.let { (manager.activityPermissions + manager.nutritionPermission) - it }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(available, refresh) {
        granted = if (available) manager.grantedPermissions() else emptySet()
    }
    val launcher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        if (inTutorial) onDismiss() else refresh++
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Health Connect") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose whether IZUN can read activity for your dashboard and write confirmed meals to Health Connect. Meal logging works without either permission.")
                if (!available) {
                    Text("Health Connect is unavailable on this device.")
                } else {
                    granted?.let { permissions ->
                        val activityCount = manager.activityPermissions.count { it in permissions }
                        Text("Activity access · $activityCount of ${manager.activityPermissions.size} allowed")
                        Text(if (manager.nutritionPermission in permissions) "Nutrition writes · Allowed" else "Nutrition writes · Off")
                    }
                    if (!inTutorial) {
                        TextButton(onClick = {
                            val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, "Open Health Connect from Android settings.", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Manage access in Android") }
                    }
                }
            }
        },
        confirmButton = {
            if (available && !missing.isNullOrEmpty()) {
                TextButton(onClick = { launcher.launch(missing) }) { Text("Choose permissions") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (!inTutorial) "Close" else if (available && missing?.isEmpty() != true) "Skip" else "Finish")
            }
        }
    )
}

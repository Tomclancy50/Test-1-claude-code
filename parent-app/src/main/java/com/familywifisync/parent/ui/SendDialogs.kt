package com.familywifisync.parent.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familywifisync.parent.bluetooth.BtDevice

/** Lets the parent pick which (paired or just-discovered) Kindle to send to. */
@Composable
fun DevicePickerDialog(
    devices: List<BtDevice>,
    scanning: Boolean,
    onScan: () -> Unit,
    onPair: (BtDevice) -> Unit,
    onPick: (BtDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Kindle") },
        text = {
            Column {
                Text(
                    "Pick a paired tablet. If it's not listed, tap Scan and pair it " +
                        "(accept the pairing request on both screens).",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.padding(4.dp))
                if (devices.isEmpty()) {
                    Text(
                        if (scanning) "Scanning…" else "No devices yet. Tap Scan.",
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(devices, key = { it.address }) { device ->
                            DeviceRow(
                                device = device,
                                onClick = { if (device.bonded) onPick(device) else onPair(device) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onScan, enabled = !scanning) {
                if (scanning) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.padding(4.dp))
                }
                Text(if (scanning) "Scanning" else "Scan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun DeviceRow(device: BtDevice, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(device.name, fontWeight = FontWeight.Medium)
            Text(
                if (device.bonded) "Paired · tap to send" else "Tap to pair",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Shows progress, then the success/failure outcome of a send. */
@Composable
fun SendResultDialog(state: SendState, onDismiss: () -> Unit) {
    when (state) {
        is SendState.Sending -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Sending…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.padding(8.dp))
                    Text("Connecting to ${state.deviceName}")
                }
            },
            confirmButton = {},
        )

        is SendState.Success -> {
            val r = state.result
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32)) },
                title = { Text("Sent to ${r.deviceName}") },
                text = {
                    Column {
                        Text("Installed ${r.installed} of ${r.received} networks.")
                        if (r.needsApproval) {
                            Spacer(Modifier.padding(4.dp))
                            Text(
                                "On this Kindle (newer Fire OS), the tablet will ask once " +
                                    "to allow each suggested network the first time it's in " +
                                    "range — tap allow and it'll auto-connect after that.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (r.message.isNotBlank()) {
                            Spacer(Modifier.padding(4.dp))
                            Text(r.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
            )
        }

        is SendState.Failure -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Couldn't send") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        )

        SendState.Idle -> Unit
    }
}

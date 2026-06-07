package com.familywifisync.parent.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.familywifisync.shared.model.WifiNetwork
import com.familywifisync.shared.model.WifiSecurity

/**
 * Dialog to add a new network or edit an existing one. When adding (no
 * [existing]), the SSID is pre-filled with the phone's currently connected
 * network to save typing.
 */
@Composable
fun NetworkEditorDialog(
    existing: WifiNetwork?,
    suggestedSsid: () -> String,
    onDismiss: () -> Unit,
    onSave: (WifiNetwork) -> Unit,
) {
    var ssid by remember { mutableStateOf(existing?.ssid ?: suggestedSsid()) }
    var password by remember { mutableStateOf(existing?.password ?: "") }
    var security by remember { mutableStateOf(existing?.security ?: WifiSecurity.WPA) }
    var hidden by remember { mutableStateOf(existing?.hidden ?: false) }
    var showPassword by remember { mutableStateOf(false) }

    val candidate = WifiNetwork(ssid.trim(), password, security, hidden)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add network" else "Edit network") },
        text = {
            Column {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("Network name (SSID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                Text("Security", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                Row {
                    SecurityOption("WPA/WPA2/WPA3", security == WifiSecurity.WPA) { security = WifiSecurity.WPA }
                    SecurityOption("Open", security == WifiSecurity.OPEN) { security = WifiSecurity.OPEN }
                    SecurityOption("WEP", security == WifiSecurity.WEP) { security = WifiSecurity.WEP }
                }

                if (security != WifiSecurity.OPEN) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showPassword, onCheckedChange = { showPassword = it })
                        Text("Show password")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hidden, onCheckedChange = { hidden = it })
                    Text("Hidden network (doesn't broadcast its name)")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = candidate.isValid(),
                onClick = { onSave(candidate) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SecurityOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.selectable(selected = selected, onClick = onClick).padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
}

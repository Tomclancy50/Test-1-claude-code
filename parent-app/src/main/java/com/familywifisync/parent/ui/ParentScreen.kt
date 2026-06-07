package com.familywifisync.parent.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familywifisync.shared.model.WifiNetwork
import com.familywifisync.shared.model.WifiSecurity

/**
 * Top-level parent screen. The hosting [MainActivity] supplies callbacks that
 * are gated on runtime permissions (opening the device picker, sending), so the
 * pure UI here stays permission-agnostic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentScreen(
    networks: List<WifiNetwork>,
    bluetoothReady: Boolean,
    onSaveNetwork: (WifiNetwork) -> Unit,
    onDeleteNetwork: (WifiNetwork) -> Unit,
    onOpenSend: () -> Unit,
    suggestedSsid: () -> String,
) {
    var editing by remember { mutableStateOf<WifiNetwork?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiFi networks to share") },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "How to use")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add network") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!bluetoothReady) {
                BluetoothWarning()
            }

            if (networks.isEmpty()) {
                EmptyState(Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(networks, key = { it.key }) { network ->
                        NetworkRow(
                            network = network,
                            onEdit = { editing = network; showAdd = true },
                            onDelete = { onDeleteNetwork(network) },
                        )
                        HorizontalDivider()
                    }
                }
            }

            SendBar(
                enabled = networks.isNotEmpty(),
                onSend = onOpenSend,
            )
        }
    }

    if (showAdd) {
        NetworkEditorDialog(
            existing = editing,
            suggestedSsid = suggestedSsid,
            onDismiss = { showAdd = false },
            onSave = {
                onSaveNetwork(it)
                showAdd = false
            },
        )
    }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }
}

/** Step-by-step usage instructions, opened from the toolbar's help icon. */
@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How to use") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                HelpStep("1. Add your networks", "Tap \"Add network\" and enter the WiFi name and password. The name is pre-filled with the network you're on right now, so usually you just type the password.")
                HelpStep("Tip: find a password", "On your phone, Settings → WiFi → tap the network → Share shows its password and a QR code.")
                HelpStep("2. Pair the Kindle (first time only)", "On the Kindle, open WiFi Sync — Kid and tap \"Make findable\". Here, tap \"Send to a Kindle\" → Scan → tap the tablet, then accept pairing on both screens.")
                HelpStep("3. Send", "On the Kindle tap \"Ready to receive\", then here tap \"Send to a Kindle\" and pick the tablet. Networks transfer over Bluetooth.")
                HelpStep("4. On the tablet", "Older Fire tablets connect automatically. Newer ones show a one-time \"allow this network?\" prompt the first time you're in range — tap allow and it auto-connects after that.")
                HelpStep("Why type passwords?", "Android won't let any app read your saved WiFi passwords unless the phone is rooted — so you enter each one once. Passwords are stored encrypted and only sent over a paired Bluetooth link.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
    )
}

@Composable
private fun HelpStep(heading: String, body: String) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(heading, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(body, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NetworkRow(
    network: WifiNetwork,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(network.ssid, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
            val subtitle = buildString {
                append(
                    when (network.security) {
                        WifiSecurity.OPEN -> "Open"
                        WifiSecurity.WPA -> "WPA/WPA2/WPA3"
                        WifiSecurity.WEP -> "WEP"
                    },
                )
                if (network.hidden) append(" · Hidden")
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            imageVector = if (network.security == WifiSecurity.OPEN) Icons.Default.LockOpen else Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 16.dp).clickable(onClick = onDelete),
        )
    }
}

@Composable
private fun SendBar(enabled: Boolean, onSend: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Ready to send ${if (enabled) "" else "(add a network first)"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "On the Kindle, open WiFi Sync — Kid and tap \"Ready to receive\". " +
                    "Then send below.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            ExtendedFloatingActionButton(
                onClick = { if (enabled) onSend() },
                icon = { Icon(Icons.Default.Send, contentDescription = null) },
                text = { Text("Send to a Kindle") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BluetoothWarning() {
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            "Bluetooth is off or not permitted. Turn on Bluetooth (and allow the " +
                "permission) to send networks to a Kindle.",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Wifi,
            contentDescription = null,
            modifier = Modifier.height(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
        Text("No networks yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap \"Add network\" to enter a WiFi name and password you want the " +
                "kids' tablets to know.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

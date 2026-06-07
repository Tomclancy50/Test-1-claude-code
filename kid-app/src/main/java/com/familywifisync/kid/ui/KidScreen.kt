package com.familywifisync.kid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidScreen(
    state: ReceiveState,
    bluetoothReady: Boolean,
    onMakeDiscoverable: () -> Unit,
    onStartReceiving: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive WiFi from parent") },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "How to use")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!bluetoothReady) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Turn on Bluetooth and allow the permission to receive networks.",
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            when (state) {
                ReceiveState.Idle -> IdleContent(onMakeDiscoverable, onStartReceiving)
                ReceiveState.Listening -> ListeningContent(onStop)
                is ReceiveState.Done -> DoneContent(state, onReset)
                is ReceiveState.Error -> ErrorContent(state.message, onReset)
            }
        }
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
                HelpStep("What this does", "It copies WiFi networks from a parent's phone to this tablet over Bluetooth, so the tablet can join them automatically.")
                HelpStep("1. Pair (first time only)", "Tap \"Make findable (pairing)\". On the parent's phone, open WiFi Sync — Parent, tap \"Send to a Kindle\" → Scan, pick this tablet, and accept pairing on both screens.")
                HelpStep("2. Get ready", "Tap \"Ready to receive\". The tablet waits for the phone to connect.")
                HelpStep("3. Send from the phone", "On the phone tap \"Send to a Kindle\", pick this tablet, and tap Send. The networks transfer in a couple of seconds.")
                HelpStep("4. Connecting", "On older Fire tablets the networks connect automatically. On newer ones, a one-time \"allow this network?\" prompt appears the first time you're in range — tap allow, and it connects on its own afterwards.")
                HelpStep("Keep this app installed", "On newer Fire OS the tablet relies on this app for the saved networks, so don't uninstall it.")
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
private fun IdleContent(onMakeDiscoverable: () -> Unit, onStart: () -> Unit) {
    Icon(
        Icons.Default.Wifi,
        contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Text("Get this tablet onto the family WiFi", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Text(
        "1. First time only: tap \"Make findable\" and pair with the parent's phone.\n" +
            "2. Then tap \"Ready to receive\" and have the parent send from their phone.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onMakeDiscoverable, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Bluetooth, null)
        Spacer(Modifier.size(8.dp))
        Text("Make findable (pairing)")
    }
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text("Ready to receive", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ListeningContent(onStop: () -> Unit) {
    CircularProgressIndicator()
    Text("Waiting for the parent's phone…", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    Text(
        "On the phone, open WiFi Sync — Parent, choose this Kindle, and tap Send.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    OutlinedButton(onClick = onStop) { Text("Cancel") }
}

@Composable
private fun DoneContent(state: ReceiveState.Done, onReset: () -> Unit) {
    val r = state.result
    Icon(Icons.Default.CheckCircle, null, Modifier.size(72.dp), tint = Color(0xFF2E7D32))
    Text("Got ${r.installed} network(s)!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Sent by ${r.deviceName}", style = MaterialTheme.typography.bodyMedium)
    if (r.message.isNotBlank()) {
        Card(Modifier.fillMaxWidth()) {
            Text(r.message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (r.needsApproval) {
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(12.dp))
                Text(
                    "When you're near one of these networks, tap the system notification " +
                        "to allow it once. After that it connects on its own.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}

@Composable
private fun ErrorContent(message: String, onReset: () -> Unit) {
    Text("Hmm, that didn't work", style = MaterialTheme.typography.titleMedium)
    Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
}

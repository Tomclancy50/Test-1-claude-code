package com.familywifisync.kid

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.familywifisync.kid.ui.KidScreen
import com.familywifisync.kid.ui.KidViewModel
import com.familywifisync.kid.ui.theme.FamilyWifiSyncTheme

class MainActivity : ComponentActivity() {

    private val viewModel: KidViewModel by viewModels { KidViewModel.Factory }

    // On Android 12+ receiving and being discoverable need these runtime grants.
    private val requiredPermissions: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        } else {
            emptyArray()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FamilyWifiSyncTheme {
                val state by viewModel.state.collectAsState()

                // Launchers: enable Bluetooth, become discoverable, request perms.
                val makeDiscoverable = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { /* result is the chosen duration or canceled; nothing to do. */ }

                val enableBluetooth = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { viewModel.startReceiving() }

                // Separate launchers so each action's permission result is unambiguous.
                val permsForDiscoverable = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { grants ->
                    if (grants.values.all { it }) launchDiscoverable(makeDiscoverable)
                }

                val permsForReceive = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { grants ->
                    if (grants.values.all { it }) {
                        if (viewModel.bluetoothEnabled()) {
                            viewModel.startReceiving()
                        } else {
                            enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        }
                    }
                }

                KidScreen(
                    state = state,
                    bluetoothReady = viewModel.bluetoothSupported() && viewModel.bluetoothEnabled(),
                    onMakeDiscoverable = {
                        if (requiredPermissions.isEmpty()) {
                            launchDiscoverable(makeDiscoverable)
                        } else {
                            permsForDiscoverable.launch(requiredPermissions)
                        }
                    },
                    onStartReceiving = {
                        if (requiredPermissions.isNotEmpty()) {
                            permsForReceive.launch(requiredPermissions)
                        } else if (viewModel.bluetoothEnabled()) {
                            viewModel.startReceiving()
                        } else {
                            enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        }
                    },
                    onStop = viewModel::stopReceiving,
                    onReset = viewModel::reset,
                )
            }
        }
    }

    private fun launchDiscoverable(
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    ) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
        launcher.launch(intent)
    }
}

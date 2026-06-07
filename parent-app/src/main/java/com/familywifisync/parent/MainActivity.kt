package com.familywifisync.parent

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import com.familywifisync.parent.ui.DevicePickerDialog
import com.familywifisync.parent.ui.ParentScreen
import com.familywifisync.parent.ui.ParentViewModel
import com.familywifisync.parent.ui.SendResultDialog
import com.familywifisync.parent.ui.SendState
import com.familywifisync.parent.ui.theme.FamilyWifiSyncTheme
import com.familywifisync.shared.bluetooth.BluetoothPermissions

class MainActivity : ComponentActivity() {

    private val viewModel: ParentViewModel by viewModels { ParentViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FamilyWifiSyncTheme {
                var showPicker by remember { mutableStateOf(false) }

                val networks by viewModel.networks.collectAsState()
                val devices by viewModel.devices.collectAsState()
                val scanning by viewModel.scanning.collectAsState()
                val sendState by viewModel.sendState.collectAsState()

                // Ask for Bluetooth permission, then enable BT, then open the picker.
                val enableBluetooth = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) {
                    viewModel.refreshBondedDevices()
                    showPicker = true
                }
                val requestPermissions = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { grants ->
                    if (grants.values.all { it }) {
                        if (viewModel.bluetoothEnabled()) {
                            viewModel.refreshBondedDevices()
                            showPicker = true
                        } else {
                            enableBluetooth.launch(android.content.Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        }
                    }
                }

                ParentScreen(
                    networks = networks,
                    bluetoothReady = viewModel.bluetoothSupported() && viewModel.bluetoothEnabled(),
                    onSaveNetwork = viewModel::saveNetwork,
                    onDeleteNetwork = viewModel::deleteNetwork,
                    suggestedSsid = viewModel::suggestedSsid,
                    onOpenSend = {
                        requestPermissions.launch(BluetoothPermissions.all)
                    },
                )

                if (showPicker) {
                    DevicePickerDialog(
                        devices = devices,
                        scanning = scanning,
                        onScan = viewModel::scanForDevices,
                        onPair = viewModel::pair,
                        onPick = { device ->
                            showPicker = false
                            viewModel.send(device)
                        },
                        onDismiss = { showPicker = false },
                    )
                }

                if (sendState != SendState.Idle) {
                    SendResultDialog(state = sendState, onDismiss = viewModel::clearSendState)
                }
            }
        }
    }
}

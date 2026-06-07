package com.familywifisync.parent.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familywifisync.parent.bluetooth.BluetoothSender
import com.familywifisync.parent.bluetooth.BtDevice
import com.familywifisync.parent.data.NetworkRepository
import com.familywifisync.parent.wifi.CurrentWifi
import com.familywifisync.shared.model.SyncRequest
import com.familywifisync.shared.model.SyncResult
import com.familywifisync.shared.model.WifiNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Status of an in-progress or finished send to a Kindle. */
sealed interface SendState {
    data object Idle : SendState
    data class Sending(val deviceName: String) : SendState
    data class Success(val result: SyncResult) : SendState
    data class Failure(val message: String) : SendState
}

class ParentViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NetworkRepository(app)
    private val sender = BluetoothSender(app)

    val networks: StateFlow<List<WifiNetwork>> = repository.networks

    private val _devices = MutableStateFlow<List<BtDevice>>(emptyList())
    val devices: StateFlow<List<BtDevice>> = _devices.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    fun bluetoothSupported() = sender.isSupported()
    fun bluetoothEnabled() = sender.isEnabled()

    fun saveNetwork(network: WifiNetwork) = repository.upsert(network)
    fun deleteNetwork(network: WifiNetwork) = repository.remove(network)

    /** Pre-fills the add-network form with the network the phone is on right now. */
    fun suggestedSsid(): String = CurrentWifi.connectedSsid(getApplication()) ?: ""

    /** Refreshes the bonded-device list (call when Bluetooth permissions are granted). */
    fun refreshBondedDevices() {
        _devices.value = sender.bondedDevices().sortedBy { it.name.lowercase() }
    }

    /** Runs a timed discovery scan, merging found devices into [devices]. */
    fun scanForDevices() {
        if (_scanning.value) return
        _scanning.value = true
        viewModelScope.launch {
            try {
                refreshBondedDevices()
                sender.discoverDevices().collect { found ->
                    _devices.value = (_devices.value.filterNot { it.address == found.address } + found)
                        .sortedWith(compareByDescending<BtDevice> { it.bonded }.thenBy { it.name.lowercase() })
                }
            } finally {
                _scanning.value = false
            }
        }
    }

    fun pair(device: BtDevice) {
        if (sender.bond(device.address)) {
            // Re-query shortly; the system pairing dialog resolves asynchronously.
            viewModelScope.launch { refreshBondedDevices() }
        }
    }

    fun send(device: BtDevice) {
        if (networks.value.isEmpty()) {
            _sendState.value = SendState.Failure("Add at least one WiFi network first.")
            return
        }
        _sendState.value = SendState.Sending(device.name)
        viewModelScope.launch {
            _sendState.value = try {
                val request = SyncRequest(
                    senderName = Build.MODEL ?: "Parent phone",
                    networks = networks.value,
                )
                SendState.Success(sender.send(device.address, request))
            } catch (e: SecurityException) {
                SendState.Failure("Missing Bluetooth permission.")
            } catch (e: Exception) {
                SendState.Failure(
                    "Couldn't reach ${device.name}. Make sure the Kindle app shows " +
                        "\"Ready to receive\" and the tablets are close. (${e.message})",
                )
            }
        }
    }

    fun clearSendState() {
        _sendState.value = SendState.Idle
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras,
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return ParentViewModel(app) as T
            }
        }
    }
}

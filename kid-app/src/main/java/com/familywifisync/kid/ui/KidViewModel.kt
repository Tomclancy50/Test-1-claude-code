package com.familywifisync.kid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.familywifisync.kid.bluetooth.BluetoothReceiver
import com.familywifisync.kid.wifi.WifiInstaller
import com.familywifisync.shared.model.SyncResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the kid screen is doing right now. */
sealed interface ReceiveState {
    data object Idle : ReceiveState
    data object Listening : ReceiveState
    data class Done(val result: SyncResult) : ReceiveState
    data class Error(val message: String) : ReceiveState
}

class KidViewModel(app: Application) : AndroidViewModel(app) {

    private val receiver = BluetoothReceiver(app)
    private val installer = WifiInstaller(app)

    private val _state = MutableStateFlow<ReceiveState>(ReceiveState.Idle)
    val state: StateFlow<ReceiveState> = _state.asStateFlow()

    private var listenJob: Job? = null

    fun bluetoothSupported() = receiver.isSupported()
    fun bluetoothEnabled() = receiver.isEnabled()

    fun startReceiving() {
        if (listenJob?.isActive == true) return
        _state.value = ReceiveState.Listening
        listenJob = viewModelScope.launch {
            _state.value = try {
                ReceiveState.Done(receiver.receiveOnce(installer::install))
            } catch (e: SecurityException) {
                ReceiveState.Error("Missing Bluetooth permission.")
            } catch (e: Exception) {
                ReceiveState.Error("Stopped listening. (${e.message ?: "no connection"})")
            }
        }
    }

    fun stopReceiving() {
        receiver.cancel()
        listenJob?.cancel()
        listenJob = null
        if (_state.value is ReceiveState.Listening) _state.value = ReceiveState.Idle
    }

    fun reset() {
        _state.value = ReceiveState.Idle
    }

    override fun onCleared() {
        receiver.cancel()
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return KidViewModel(app) as T
            }
        }
    }
}

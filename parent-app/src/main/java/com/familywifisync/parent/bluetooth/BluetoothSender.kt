package com.familywifisync.parent.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.familywifisync.shared.bluetooth.SppProtocol
import com.familywifisync.shared.model.SyncRequest
import com.familywifisync.shared.model.SyncResult
import com.familywifisync.shared.util.getParcelableExtraCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/** A Bluetooth device shown in the picker. */
data class BtDevice(
    val name: String,
    val address: String,
    val bonded: Boolean,
)

/**
 * Drives the parent (client) side of the Bluetooth transfer: list paired
 * devices, optionally discover new ones, and open an RFCOMM connection to push
 * the network list and read the result back.
 *
 * Permission checks live in the UI layer; the platform calls here are annotated
 * [SuppressLint] accordingly and also defend against [SecurityException].
 */
class BluetoothSender(context: Context) {

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun isSupported(): Boolean = adapter != null
    fun isEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<BtDevice> = try {
        adapter?.bondedDevices.orEmpty().map { it.toBtDevice(bonded = true) }
    } catch (_: SecurityException) {
        emptyList()
    }

    /**
     * Starts classic discovery and emits each newly found device. Collect this
     * inside a coroutine scope; cancelling the collection stops discovery and
     * unregisters the receiver.
     */
    @SuppressLint("MissingPermission")
    fun discoverDevices(): Flow<BtDevice> = callbackFlow {
        val adapter = adapter ?: run { close(); return@callbackFlow }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothDevice.ACTION_FOUND) {
                    val device: BluetoothDevice? = intent.getParcelableExtraCompat(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java,
                    )
                    device?.let { trySend(it.toBtDevice(bonded = it.bondState == BluetoothDevice.BOND_BONDED)) }
                }
            }
        }

        appContext.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        try {
            adapter.cancelDiscovery()
            adapter.startDiscovery()
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            runCatching { adapter.cancelDiscovery() }
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }

    /** Requests a system pairing with [address]. Returns false if it couldn't start. */
    @SuppressLint("MissingPermission")
    fun bond(address: String): Boolean = try {
        adapter?.getRemoteDevice(address)?.createBond() ?: false
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    /**
     * Connects to [address] over RFCOMM, sends [request], and returns the
     * [SyncResult] the kid app replies with. Runs on the IO dispatcher; throws
     * IOException/SecurityException on failure for the caller to surface.
     */
    @SuppressLint("MissingPermission")
    suspend fun send(address: String, request: SyncRequest): SyncResult =
        withContext(Dispatchers.IO) {
            val adapter = adapter ?: error("Bluetooth not available")
            val device = adapter.getRemoteDevice(address)
            // Discovery murders connection throughput; always stop it first.
            runCatching { adapter.cancelDiscovery() }
            val socket = device.createRfcommSocketToServiceRecord(SppProtocol.SERVICE_UUID)
            socket.use {
                it.connect()
                SppProtocol.writeRequest(it.outputStream, request)
                SppProtocol.readResult(it.inputStream)
            }
        }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toBtDevice(bonded: Boolean) = BtDevice(
        name = try { name } catch (_: SecurityException) { null } ?: "Unknown device",
        address = address,
        bonded = bonded,
    )
}

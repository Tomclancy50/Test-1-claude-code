package com.familywifisync.kid.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.content.Context
import com.familywifisync.kid.wifi.InstallOutcome
import com.familywifisync.shared.bluetooth.SppProtocol
import com.familywifisync.shared.model.SyncRequest
import com.familywifisync.shared.model.SyncResult
import com.familywifisync.shared.model.WifiNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The kid (server) side of the transfer. Opens an RFCOMM service, waits for the
 * parent phone to connect, installs the networks it sends, and replies with a
 * [SyncResult]. One connection per "Ready to receive" tap.
 */
class BluetoothReceiver(context: Context) {

    private val deviceName = android.os.Build.MODEL ?: "Kindle"
    private val adapter: BluetoothAdapter? =
        (context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE)
            as? BluetoothManager)?.adapter

    @Volatile
    private var serverSocket: BluetoothServerSocket? = null

    fun isSupported(): Boolean = adapter != null
    fun isEnabled(): Boolean = adapter?.isEnabled == true

    /**
     * Listens for one incoming connection and installs whatever it receives.
     * Blocks (on the IO dispatcher) until a parent connects or [cancel] is
     * called. The [install] lambda performs the WiFi work and returns the count.
     */
    @SuppressLint("MissingPermission")
    suspend fun receiveOnce(install: (List<WifiNetwork>) -> InstallOutcome): SyncResult =
        withContext(Dispatchers.IO) {
            val adapter = adapter ?: error("Bluetooth not available")
            val server = adapter.listenUsingRfcommWithServiceRecord(
                SppProtocol.SERVICE_NAME,
                SppProtocol.SERVICE_UUID,
            )
            serverSocket = server
            try {
                val socket = server.accept() // blocks until the phone connects
                socket.use { s ->
                    val request: SyncRequest = SppProtocol.readRequest(s.inputStream)
                    val outcome = install(request.networks)
                    val result = SyncResult(
                        deviceName = deviceName,
                        received = request.networks.size,
                        installed = outcome.installed,
                        needsApproval = outcome.needsApproval,
                        message = outcome.message,
                    )
                    SppProtocol.writeResult(s.outputStream, result)
                    result
                }
            } finally {
                runCatching { server.close() }
                serverSocket = null
            }
        }

    /** Aborts a pending [receiveOnce] by closing the listening socket. */
    fun cancel() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }
}

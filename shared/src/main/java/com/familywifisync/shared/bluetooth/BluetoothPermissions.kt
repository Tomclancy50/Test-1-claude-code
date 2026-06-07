package com.familywifisync.shared.bluetooth

import android.Manifest
import android.os.Build

/**
 * The runtime permissions each app must request before touching Bluetooth.
 *
 * Android 12 (API 31) split the old single BLUETOOTH permission into the
 * granular BLUETOOTH_CONNECT / BLUETOOTH_SCAN runtime permissions. Below 31,
 * scanning for / discovering devices requires location instead.
 */
object BluetoothPermissions {

    /** Permissions needed to connect to an already-bonded device and exchange data. */
    val connect: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        emptyArray() // BLUETOOTH is install-time on these versions.
    }

    /** Permissions needed to discover/pair new devices and become discoverable. */
    val discover: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        // Classic discovery returns nothing without location on 6.0..11.
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /** Everything the app might need, de-duplicated. Handy for a single up-front request. */
    val all: Array<String> = (connect + discover).distinct().toTypedArray()
}

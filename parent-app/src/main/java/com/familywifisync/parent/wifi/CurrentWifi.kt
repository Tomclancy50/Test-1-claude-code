package com.familywifisync.parent.wifi

import android.content.Context
import android.net.wifi.WifiManager

/**
 * Best-effort read of the *currently connected* WiFi network name, used only to
 * pre-fill the "add network" form so the parent types less.
 *
 * Note: Android does NOT let a normal app read *saved* passwords or even the
 * full list of saved SSIDs (that needs root). The current SSID is the one thing
 * available, and only while connected and with location permission granted.
 */
object CurrentWifi {

    fun connectedSsid(context: Context): String? {
        val wifi = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        @Suppress("DEPRECATION") // connectionInfo is the only API that exposes the SSID here.
        val info = wifi.connectionInfo ?: return null
        val ssid = info.ssid ?: return null
        // The framework wraps the SSID in quotes; <unknown ssid> means "not available".
        return ssid.trim('"').takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    }
}

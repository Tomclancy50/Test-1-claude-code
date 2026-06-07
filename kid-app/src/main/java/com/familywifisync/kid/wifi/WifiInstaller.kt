package com.familywifisync.kid.wifi

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.annotation.RequiresApi
import com.familywifisync.shared.model.WifiNetwork
import com.familywifisync.shared.model.WifiSecurity

/** Result of installing a batch of networks, surfaced back to the parent. */
data class InstallOutcome(
    val installed: Int,
    val needsApproval: Boolean,
    val message: String,
)

/**
 * Adds received WiFi networks to this device so it auto-connects in range.
 *
 * Android changed the rules at version 10 (API 29):
 *  - Older Fire OS / Android (< 29): we can use [WifiManager.addNetwork] with a
 *    [WifiConfiguration]; the network is saved and the device connects silently.
 *  - Newer Fire OS / Android (>= 29): third-party [WifiManager.addNetwork] is
 *    blocked, so we use the [WifiNetworkSuggestion] API instead. The OS shows a
 *    one-time approval the first time each network is in range, then
 *    auto-connects. (This is the best a non-system, non-root app can do.)
 */
class WifiInstaller(context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun install(networks: List<WifiNetwork>): InstallOutcome =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            installViaSuggestions(networks)
        } else {
            installViaLegacy(networks)
        }

    // --- Android 9 and older: direct, fully automatic. ----------------------

    @Suppress("DEPRECATION")
    private fun installViaLegacy(networks: List<WifiNetwork>): InstallOutcome {
        if (!wifiManager.isWifiEnabled) wifiManager.isWifiEnabled = true
        var installed = 0
        for (n in networks) {
            val config = WifiConfiguration().apply {
                SSID = quote(n.ssid)
                hiddenSSID = n.hidden
                when (n.security) {
                    WifiSecurity.OPEN ->
                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)

                    WifiSecurity.WPA ->
                        preSharedKey = quote(n.password)

                    WifiSecurity.WEP -> {
                        wepKeys[0] = quote(n.password)
                        wepTxKeyIndex = 0
                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                        allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                    }
                }
            }
            val netId = wifiManager.addNetwork(config)
            if (netId != -1) {
                wifiManager.enableNetwork(netId, /* attemptConnect = */ false)
                installed++
            }
        }
        wifiManager.saveConfiguration()
        return InstallOutcome(
            installed = installed,
            needsApproval = false,
            message = if (installed == networks.size) {
                "All networks saved. The tablet will connect automatically in range."
            } else {
                "Saved $installed of ${networks.size}. Some may already exist."
            },
        )
    }

    // --- Android 10+: suggestion API, one-time approval. --------------------

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun installViaSuggestions(networks: List<WifiNetwork>): InstallOutcome {
        // Replace this app's previous suggestions so re-sending updates cleanly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { wifiManager.removeNetworkSuggestions(wifiManager.networkSuggestions) }
        }

        val skippedWep = networks.count { it.security == WifiSecurity.WEP }
        val suggestions = networks
            .filter { it.security != WifiSecurity.WEP } // WEP isn't supported by this API.
            .map { n ->
                WifiNetworkSuggestion.Builder()
                    .setSsid(n.ssid)
                    .apply {
                        setIsHiddenSsid(n.hidden)
                        if (n.security == WifiSecurity.WPA) setWpa2Passphrase(n.password)
                    }
                    .build()
            }

        val status = if (suggestions.isEmpty()) {
            WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
        } else {
            wifiManager.addNetworkSuggestions(suggestions)
        }

        val ok = status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS ||
            status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE
        val installed = if (ok) suggestions.size else 0

        val message = buildString {
            if (!ok) append("WiFi suggestion error (code $status). ")
            if (skippedWep > 0) append("Skipped $skippedWep WEP network(s) — unsupported on this Fire OS. ")
            if (ok) append("The tablet will ask once to allow each network in range, then auto-connect.")
        }.trim()

        return InstallOutcome(
            installed = installed,
            needsApproval = ok,
            message = message,
        )
    }

    private fun quote(value: String) = "\"" + value + "\""
}

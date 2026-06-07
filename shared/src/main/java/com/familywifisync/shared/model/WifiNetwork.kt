package com.familywifisync.shared.model

import kotlinx.serialization.Serializable

/**
 * The security scheme of a WiFi network. We only model what consumer home
 * routers actually use; enterprise (802.1x) networks can't be provisioned this
 * way and are intentionally unsupported.
 */
@Serializable
enum class WifiSecurity {
    /** Open network, no password. */
    OPEN,

    /** WPA / WPA2 / WPA3 personal (PSK passphrase). The common home case. */
    WPA,

    /** Legacy WEP. Rare and insecure, but some old gear still uses it. */
    WEP,
}

/**
 * A single WiFi network the parent wants the kids' tablets to know about.
 *
 * @param ssid the network name, exactly as it appears (case sensitive).
 * @param password the passphrase. Empty when [security] is [WifiSecurity.OPEN].
 * @param security how the network is secured.
 * @param hidden true if the network does not broadcast its SSID.
 */
@Serializable
data class WifiNetwork(
    val ssid: String,
    val password: String = "",
    val security: WifiSecurity = WifiSecurity.WPA,
    val hidden: Boolean = false,
) {
    /** A stable identity for a network is its name plus whether it's hidden. */
    val key: String get() = "$ssid|$hidden"

    fun isValid(): Boolean {
        if (ssid.isBlank()) return false
        return when (security) {
            WifiSecurity.OPEN -> true
            // WPA personal passphrases are 8..63 ASCII chars (or a 64-hex PSK).
            WifiSecurity.WPA -> password.length in 8..64
            // WEP keys are 5 or 13 ASCII chars (or 10/26 hex).
            WifiSecurity.WEP -> password.isNotEmpty()
        }
    }
}

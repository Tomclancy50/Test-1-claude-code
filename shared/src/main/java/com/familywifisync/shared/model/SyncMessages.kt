package com.familywifisync.shared.model

import kotlinx.serialization.Serializable

/**
 * Sent parent -> kid: the batch of networks to install. [version] lets the two
 * apps detect a protocol mismatch if they're ever updated independently.
 */
@Serializable
data class SyncRequest(
    val version: Int = PROTOCOL_VERSION,
    val senderName: String,
    val networks: List<WifiNetwork>,
)

/**
 * Sent kid -> parent after installation, so the parent can show a real result
 * instead of guessing. [needsApproval] is true on Android 10+/newer Fire OS,
 * where the system shows a one-time notification the child (or parent) must
 * accept before the tablet will auto-connect.
 */
@Serializable
data class SyncResult(
    val version: Int = PROTOCOL_VERSION,
    val deviceName: String,
    val received: Int,
    val installed: Int,
    val needsApproval: Boolean,
    val message: String = "",
)

const val PROTOCOL_VERSION = 1

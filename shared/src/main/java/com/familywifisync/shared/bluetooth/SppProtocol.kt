package com.familywifisync.shared.bluetooth

import com.familywifisync.shared.model.SyncRequest
import com.familywifisync.shared.model.SyncResult
import kotlinx.serialization.json.Json
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * The shared Bluetooth Classic (RFCOMM / Serial Port Profile) protocol used by
 * both apps.
 *
 * Wire format is dead simple and stream-friendly: a 4-byte big-endian length
 * prefix followed by that many bytes of UTF-8 JSON. The parent connects as the
 * client and sends a [SyncRequest]; the kid (server) installs the networks and
 * replies with a single [SyncResult].
 *
 * Bluetooth bonding (pairing) provides the link-layer encryption, so we don't
 * roll our own crypto over the channel.
 */
object SppProtocol {

    /**
     * A fixed, application-specific UUID. Both apps advertise/look up this same
     * value so they find each other's service and ignore unrelated SPP devices.
     */
    val SERVICE_UUID: UUID = UUID.fromString("7f8d2b1e-3c4a-4e6f-9a1b-2c3d4e5f6071")

    /** Human-readable SDP service name the kid app registers. */
    const val SERVICE_NAME = "FamilyWifiSync"

    /** Guardrail so a malformed/hostile length prefix can't allocate huge buffers. */
    private const val MAX_MESSAGE_BYTES = 1 shl 20 // 1 MiB is far more than enough.

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun writeRequest(out: OutputStream, request: SyncRequest) {
        writeFrame(out, json.encodeToString(SyncRequest.serializer(), request))
    }

    fun readRequest(input: InputStream): SyncRequest =
        json.decodeFromString(SyncRequest.serializer(), readFrame(input))

    fun writeResult(out: OutputStream, result: SyncResult) {
        writeFrame(out, json.encodeToString(SyncResult.serializer(), result))
    }

    fun readResult(input: InputStream): SyncResult =
        json.decodeFromString(SyncResult.serializer(), readFrame(input))

    private fun writeFrame(out: OutputStream, payload: String) {
        val bytes = payload.toByteArray(Charsets.UTF_8)
        val data = DataOutputStream(out)
        data.writeInt(bytes.size)
        data.write(bytes)
        data.flush()
    }

    private fun readFrame(input: InputStream): String {
        val data = DataInputStream(input)
        val length = data.readInt()
        if (length <= 0 || length > MAX_MESSAGE_BYTES) {
            throw IOException("Bad frame length: $length")
        }
        val bytes = ByteArray(length)
        data.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}

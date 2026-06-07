package com.familywifisync.parent.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.familywifisync.shared.model.WifiNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Stores the parent's curated list of WiFi networks.
 *
 * Passwords are sensitive, so the list is persisted in
 * [EncryptedSharedPreferences] (AES-256, key held in the Android Keystore)
 * rather than plain prefs. The repository keeps an in-memory [StateFlow] the UI
 * observes.
 */
class NetworkRepository(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _networks = MutableStateFlow(load())
    val networks: StateFlow<List<WifiNetwork>> = _networks.asStateFlow()

    /** Adds a new network or replaces an existing one with the same [WifiNetwork.key]. */
    fun upsert(network: WifiNetwork) {
        val updated = _networks.value.filterNot { it.key == network.key } + network
        save(updated.sortedBy { it.ssid.lowercase() })
    }

    fun remove(network: WifiNetwork) {
        save(_networks.value.filterNot { it.key == network.key })
    }

    private fun load(): List<WifiNetwork> {
        val raw = prefs.getString(KEY_NETWORKS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer, raw)
        }.getOrDefault(emptyList())
    }

    private fun save(list: List<WifiNetwork>) {
        prefs.edit()
            .putString(KEY_NETWORKS, json.encodeToString(ListSerializer, list))
            .apply()
        _networks.value = list
    }

    private companion object {
        const val PREFS_FILE = "networks.secure"
        const val KEY_NETWORKS = "networks"
        val ListSerializer =
            kotlinx.serialization.builtins.ListSerializer(WifiNetwork.serializer())
    }
}

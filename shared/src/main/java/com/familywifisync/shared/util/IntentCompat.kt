package com.familywifisync.shared.util

import android.content.Intent
import android.os.Build
import android.os.Parcelable

/**
 * Version-safe replacement for the deprecated [Intent.getParcelableExtra]. On
 * API 33+ it uses the typed overload; below that it falls back to the old one.
 */
@Suppress("DEPRECATION")
fun <T : Parcelable> Intent.getParcelableExtraCompat(name: String, clazz: Class<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, clazz)
    } else {
        getParcelableExtra(name) as? T
    }

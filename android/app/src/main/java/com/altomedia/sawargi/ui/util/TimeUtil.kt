package com.altomedia.sawargi.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Convert an ISO-8601 timestamp into a short relative Indonesian string. */
fun relativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val parsed = runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso)
                ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US).parse(iso)
        }.getOrNull()
        if (parsed == null) return iso
        val diff = System.currentTimeMillis() - parsed.time
        val seconds = diff / 1000
        when {
            seconds < 60 -> "baru saja"
            seconds < 3600 -> "${seconds / 60} mnt"
            seconds < 86400 -> "${seconds / 3600} jam"
            seconds < 604800 -> "${seconds / 86400} hr"
            else -> SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(parsed.time))
        }
    } catch (_: Exception) {
        iso
    }
}
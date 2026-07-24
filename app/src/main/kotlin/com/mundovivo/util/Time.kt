package com.mundovivo.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitários de tempo
 */
object Time {
    fun nowMillis(): Long = System.currentTimeMillis()

    fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}

package com.mundovivo.util

import kotlin.math.roundToInt

/**
 * Utilitário para formatação de RAM
 */
object RamFormatter {

    fun formatMB(mb: Long): String {
        return when {
            mb >= 1024 -> {
                val gb = mb / 1024.0
                "${gb.roundToInt()}GB"
            }
            else -> "${mb}MB"
        }
    }

    fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024 * 1024)
        return formatMB(mb)
    }
}

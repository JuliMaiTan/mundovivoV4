package com.mundovivo.llm

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Identificador único do modelo
 */
@Parcelize
data class ModelId(val value: String) : Parcelable {
    override fun toString() = value
}

/**
 * Família de modelos
 */
enum class ModelFamily {
    QWEN,
    GEMMA,
    LLAMA,
    OTHER
}

/**
 * Template de chat
 */
enum class ChatTemplate {
    CHATML,      // <|im_start|>system\n...\n<|im_end|>
    GEMMA,       // <start_of_turn>user\n...\n<end_of_turn>
    LLAMA3,      // <|start_header_id|>system<|end_header_id|>
    ALPACA       // ### Instruction:\n...\n### Response:
}

/**
 * Informações completas sobre um modelo
 */
@Parcelize
data class ModelInfo(
    val id: ModelId,
    val name: String,
    val family: ModelFamily,
    val sizeBytes: Long,
    val template: ChatTemplate,
    val downloadUrl: String,
    val sha256: String,
    val minRamMB: Int,
    val recommendedRamMB: Int,
    val abliterated: Boolean,
    val description: String
) : Parcelable {
    val sizeMB: Long get() = sizeBytes / (1024 * 1024)
    val sizeGB: Double get() = sizeBytes / (1024.0 * 1024.0 * 1024.0)
}

/**
 * Status do modelo no dispositivo
 */
enum class ModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    VERIFYING,
    READY,
    CORRUPTED,
    ERROR
}

/**
 * Progresso de download
 */
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Long
) {
    val percentage: Int get() = ((downloadedBytes * 100) / totalBytes).toInt()
    val speedMBps: Double get() = speedBytesPerSecond / (1024.0 * 1024.0)
}

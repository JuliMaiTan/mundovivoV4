package com.mundovivo.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import androidx.core.content.getSystemService

/**
 * Informações sobre o dispositivo para decisões de modelo e performance.
 */
class DeviceInfo(private val context: Context) {

    /**
     * RAM total em MB
     */
    val totalRamMB: Long by lazy {
        val activityManager = context.getSystemService<ActivityManager>()
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        memInfo.totalMem / (1024 * 1024)
    }

    /**
     * RAM disponível em MB (snapshot atual)
     */
    val availableRamMB: Long
        get() {
            val activityManager = context.getSystemService<ActivityManager>()
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            return memInfo.availMem / (1024 * 1024)
        }

    /**
     * Espaço livre em storage interno (GB)
     */
    val storageFreeGB: Long
        get() {
            val stat = StatFs(context.filesDir.path)
            return stat.availableBytes / (1024 * 1024 * 1024)
        }

    /**
     * ABI primária do dispositivo (arm64-v8a, armeabi-v7a, x86_64)
     */
    val abi: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

    /**
     * Versão do Android (SDK int)
     */
    val androidVersion: Int = Build.VERSION.SDK_INT

    /**
     * Modelo do dispositivo (ex: "Samsung SM-G998B")
     */
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"

    /**
     * Summary formatado para debug
     */
    override fun toString(): String {
        return """DeviceInfo(
            |  RAM: ${totalRamMB}MB total, ${availableRamMB}MB disponível
            |  Storage: ${storageFreeGB}GB livre
            |  ABI: $abi
            |  Android: $androidVersion
            |  Modelo: $deviceModel
            |)""".trimMargin()
    }
}

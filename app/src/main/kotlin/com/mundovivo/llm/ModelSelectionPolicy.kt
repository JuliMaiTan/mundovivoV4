package com.mundovivo.llm

import com.mundovivo.util.DeviceInfo

/**
 * Recomendação de modelo com base no dispositivo
 */
data class ModelRecommendation(
    val recommended: ModelInfo,
    val alternatives: List<Pair<ModelInfo, String>>, // modelo + warning
    val warnings: List<String>
)

/**
 * Política de seleção de modelo baseada em RAM do dispositivo.
 * 
 * Baseado em POC real:
 * - Qwen 1.5B: 1.56 GB PSS, viável em 4GB
 * - Gemma 2B: 2.7 GB PSS, risco em 4GB, viável em 8GB+
 */
class ModelSelectionPolicy(private val deviceInfo: DeviceInfo) {

    fun getRecommendation(): ModelRecommendation {
        val ramMB = deviceInfo.totalRamMB

        return when {
            // 4GB-5GB: Apenas Qwen seguro
            ramMB < 5000 -> ModelRecommendation(
                recommended = ModelCatalog.QWEN_1_5B,
                alternatives = listOf(
                    ModelCatalog.GEMMA_2B to "⚠️ Gemma 2B pode causar lentidão ou travamentos em ${deviceInfo.totalRamMB}MB RAM"
                ),
                warnings = listOf(
                    "Dispositivo com ${deviceInfo.totalRamMB}MB RAM.",
                    "Qwen 1.5B é a escolha mais segura."
                )
            )

            // 6GB-7GB: Qwen recomendado, Gemma permitido com aviso
            ramMB < 7000 -> ModelRecommendation(
                recommended = ModelCatalog.QWEN_1_5B,
                alternatives = listOf(
                    ModelCatalog.GEMMA_2B to "⚠️ Gemma 2B pode ser mais lento neste dispositivo"
                ),
                warnings = emptyList()
            )

            // 8GB+: Ambos viáveis, Gemma sugerido para qualidade
            else -> ModelRecommendation(
                recommended = ModelCatalog.QWEN_1_5B,
                alternatives = listOf(
                    ModelCatalog.GEMMA_2B to "📖 Narrativa mais densa, recomendado para este dispositivo"
                ),
                warnings = emptyList()
            )
        }
    }

    /**
     * Verifica se modelo é seguro para o dispositivo
     */
    fun isSafe(model: ModelInfo): Boolean {
        return deviceInfo.totalRamMB >= model.minRamMB
    }

    /**
     * Retorna warnings específicos para um modelo
     */
    fun getWarnings(model: ModelInfo): List<String> {
        val warnings = mutableListOf<String>()
        val ramMB = deviceInfo.totalRamMB

        if (ramMB < model.minRamMB) {
            warnings.add("⚠️ RAM insuficiente: mínimo ${model.minRamMB}MB, disponível ${ramMB}MB")
        } else if (ramMB < model.recommendedRamMB) {
            warnings.add("⚠️ Abaixo da RAM recomendada (${model.recommendedRamMB}MB)")
        }

        val storageFreeGB = deviceInfo.storageFreeGB
        val modelSizeGB = model.sizeGB
        if (storageFreeGB < modelSizeGB + 1) { // +1GB margem
            warnings.add("⚠️ Espaço livre insuficiente: necessário ${modelSizeGB.toInt() + 1}GB, disponível ${storageFreeGB}GB")
        }

        return warnings
    }
}

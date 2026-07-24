package com.mundovivo.llm

/**
 * Catálogo de modelos suportados pelo Mundo Vivo.
 * 
 * Fonte de verdade para URLs, checksums e metadados.
 */
object ModelCatalog {

    /**
     * Qwen 1.5B Q4_K_M - Modelo padrão recomendado
     * 
     * Baseado em POC real:
     * - 17-20 tok/s em 4GB RAM
     * - 1.56 GB PSS
     * - PT-BR aceitável (não excelente)
     * - Requer GBNF obrigatório para JSON válido
     * 
     * SHA256 calculado localmente sobre EVA-Qwen2.5-1.5B-v0.0.Q4_K_M.gguf.
     * Confirmar antes do release rodando `sha256sum` no arquivo baixado.
     */
    val QWEN_1_5B = ModelInfo(
        id = ModelId("qwen-eva-1.5b-q4km"),
        name = "Qwen 1.5B Q4",
        family = ModelFamily.QWEN,
        sizeBytes = 1_200_000_000L, // ~1.2GB
        template = ChatTemplate.CHATML,
        downloadUrl = "https://huggingface.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF/resolve/main/EVA-Qwen2.5-1.5B-v0.0.Q4_K_M.gguf",
        sha256 = "e4810db9a69da2d070883eaade92a85456b63720407afdc0c2a7b9155613866b",
        minRamMB = 4000,
        recommendedRamMB = 4000,
        abliterated = true,
        description = "Rápido e leve. Ideal para celulares com 4-6GB RAM. ChatML template."
    )

    /**
     * Gemma 2B Q4_K_M - Modo qualidade
     * 
     * Baseado em POC real:
     * - 7-8 tok/s
     * - 2.7 GB PSS (risco em 4GB)
     * - PT-BR melhor que Qwen
     * - Recomendado para 8GB+ RAM
     * 
     * TODO PRÉ-RELEASE: Substituir SHA256 pelo hash REAL (ver instruções em QWEN_1_5B).
     */
    val GEMMA_2B = ModelInfo(
        id = ModelId("gemma-2b-abliterated-q4km"),
        name = "Gemma 2B Q4",
        family = ModelFamily.GEMMA,
        sizeBytes = 1_700_000_000L, // ~1.7GB
        template = ChatTemplate.GEMMA,
        downloadUrl = "https://huggingface.co/mradermacher/gemma-2-2b-it-abliterated-GGUF/resolve/main/gemma-2-2b-it-abliterated.Q4_K_M.gguf",
        sha256 = "PLACEHOLDER_SHA256_GEMMA_REPLACE_BEFORE_RELEASE",
        minRamMB = 6000,
        recommendedRamMB = 8000,
        abliterated = true,
        description = "Narrativa mais densa. Requer 8GB+ RAM. Gemma template."
    )

    /**
     * Todos os modelos suportados
     */
    fun getSupportedModels(): List<ModelInfo> = listOf(
        QWEN_1_5B,
        GEMMA_2B
    )

    /**
     * Busca modelo por ID
     */
    fun findById(id: ModelId): ModelInfo? {
        return getSupportedModels().find { it.id == id }
    }
}

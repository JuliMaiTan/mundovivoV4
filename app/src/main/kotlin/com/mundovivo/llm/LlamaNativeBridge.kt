package com.mundovivo.llm

/**
 * Bridge JNI para llama.cpp.
 * 
 * Fase 0: Stub preparado.
 * Implementação completa: próxima etapa após integração llama.cpp.
 */
object LlamaNativeBridge {

    init {
        try {
            System.loadLibrary("mundovivo-llama")
        } catch (e: UnsatisfiedLinkError) {
            // TODO: Biblioteca ainda não compilada, ok na Fase 0
        }
    }

    /**
     * Inicializa modelo.
     * @return Handle do modelo (ponteiro nativo)
     */
    external fun initModel(
        modelPath: String,
        nCtx: Int,
        nThreads: Int,
        nBatch: Int,
        grammarPath: String?
    ): Long

    /**
     * Gera texto com streaming via callback.
     */
    external fun generate(
        modelHandle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        onToken: (String) -> Unit
    ): String

    /**
     * Libera modelo da memória.
     */
    external fun freeModel(modelHandle: Long)
}

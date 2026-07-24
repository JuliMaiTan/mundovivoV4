package com.mundovivo.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Engine de inferência usando llama.cpp via JNI.
 * 
 * Fase 0: Implementação com fallback mock para testes sem native lib.
 */
class LlamaEngine(private val context: Context) {

    private var modelHandle: Long = 0L
    private var isLoaded: Boolean = false

    data class ModelConfig(
        val nCtx: Int = 2048,
        val nThreads: Int = 4,
        val nBatch: Int = 512,
        val temperature: Float = 0.7f,
        val maxTokens: Int = 500
    )

    /**
     * Carrega modelo GGUF.
     */
    suspend fun loadModel(
        modelFile: File,
        grammarFile: File?,
        config: ModelConfig = ModelConfig()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isLoaded) {
                return@withContext Result.failure(IllegalStateException("Modelo já carregado"))
            }

            if (!modelFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Modelo não existe: ${modelFile.path}"))
            }

            // TODO Fase 0: Chamar JNI real quando llama.cpp estiver integrado
            // modelHandle = LlamaNativeBridge.initModel(
            //     modelPath = modelFile.absolutePath,
            //     nCtx = config.nCtx,
            //     nThreads = config.nThreads,
            //     nBatch = config.nBatch,
            //     grammarPath = grammarFile?.absolutePath
            // )

            // MOCK para Fase 0 (permite testar UI sem native lib)
            modelHandle = 12345L // Handle mock
            isLoaded = true

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gera texto com streaming.
     * 
     * @return Flow de tokens gerados
     */
    fun generate(
        prompt: String,
        config: ModelConfig = ModelConfig()
    ): Flow<GenerationResult> = flow {
        if (!isLoaded) {
            emit(GenerationResult.Error("Modelo não carregado"))
            return@flow
        }

        try {
            val startTime = System.currentTimeMillis()
            var tokensGenerated = 0
            var firstTokenTime: Long? = null

            // TODO Fase 0: Chamar JNI real
            // val fullResponse = LlamaNativeBridge.generate(
            //     modelHandle = modelHandle,
            //     prompt = prompt,
            //     maxTokens = config.maxTokens,
            //     temperature = config.temperature
            // ) { token ->
            //     if (firstTokenTime == null) {
            //         firstTokenTime = System.currentTimeMillis()
            //     }
            //     tokensGenerated++
            //     emit(GenerationResult.Token(token))
            // }

            // MOCK para Fase 0
            firstTokenTime = System.currentTimeMillis() + 100
            val mockResponse = generateMockResponse()
            
            // Simula streaming token por token
            mockResponse.forEach { char ->
                emit(GenerationResult.Token(char.toString()))
                tokensGenerated++
                kotlinx.coroutines.delay(20) // Simula ~50 tok/s
            }

            val fullResponse = mockResponse

            // Emite resultado final com métricas
            val totalTime = System.currentTimeMillis() - startTime
            val ttft = (firstTokenTime ?: startTime) - startTime
            val tokensPerSecond = if (totalTime > 0) {
                (tokensGenerated * 1000.0) / totalTime
            } else {
                0.0
            }

            emit(
                GenerationResult.Complete(
                    fullText = fullResponse,
                    tokensGenerated = tokensGenerated,
                    tokensPerSecond = tokensPerSecond,
                    ttftMs = ttft,
                    totalTimeMs = totalTime
                )
            )
        } catch (e: Exception) {
            emit(GenerationResult.Error(e.message ?: "Erro desconhecido"))
        }
    }

    /**
     * Libera modelo da memória.
     *
     * Síncrono e non-suspend intencionalmente:
     * - A chamada JNI subjacente (llama_free_model + llama_free) é essencialmente
     *   instantânea (libera ponteiros nativos).
     * - Ser non-suspend permite chamar de dentro de ViewModel.onCleared() sem
     *   depender de viewModelScope, que já pode estar cancelado nesse momento.
     */
    fun freeModel() {
        if (isLoaded) {
            // TODO Fase 0: Chamar JNI real
            // LlamaNativeBridge.freeModel(modelHandle)

            modelHandle = 0L
            isLoaded = false
        }
    }

    /**
     * Mock response para testes Fase 0.
     */
    private fun generateMockResponse(): String {
        return """{
  "contract_version": "1.0",
  "response_type": "TURN_NARRATION",
  "narrative": "A chuva risca as janelas da taverna enquanto o taverneiro empurra uma caneca de cerveja escura pelo balcão. O líquido está morno, com espuma densa que gruda nas bordas do vidro. No canto, sob a luz fraca de uma vela que pisca, Torvin permanece imóvel. Seus olhos acompanham a cena por baixo da sombra do capuz, mas ele não se move. O vento lá fora uiva.",
  "sensory_focus": ["visao", "audicao", "olfato"],
  "npcs_mentioned": ["Torvin", "taverneiro"],
  "tone": "tensao",
  "warnings": [],
  "error": null
}"""
    }

    sealed class GenerationResult {
        data class Token(val token: String) : GenerationResult()
        data class Complete(
            val fullText: String,
            val tokensGenerated: Int,
            val tokensPerSecond: Double,
            val ttftMs: Long,
            val totalTimeMs: Long
        ) : GenerationResult()
        data class Error(val message: String) : GenerationResult()
    }
}

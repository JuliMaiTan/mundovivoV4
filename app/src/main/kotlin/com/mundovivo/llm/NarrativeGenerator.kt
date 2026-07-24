package com.mundovivo.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Gerador de narrativa que orquestra:
 * - Fatos canônicos → Prompt
 * - LLM → JSON
 * - Validação → Fallback se necessário
 */
class NarrativeGenerator(
    private val llamaEngine: LlamaEngine,
    private val promptBuilder: PromptBuilder = PromptBuilder(),
    private val validator: NarratorContractValidator = NarratorContractValidator()
) {

    /**
     * Gera narrativa a partir de fatos canônicos.
     */
    fun generate(
        facts: List<String>,
        tone: NarrativeTone,
        template: ChatTemplate,
        context: String? = null,
        config: LlamaEngine.ModelConfig = LlamaEngine.ModelConfig()
    ): Flow<NarrativeResult> = flow {
        try {
            // 1. Constrói prompt
            val prompt = promptBuilder.buildNarratorPrompt(facts, tone, template, context)
            emit(NarrativeResult.PromptBuilt(prompt))

            // 2. Gera com LLM
            var fullResponse = ""
            var lastMetrics: LlamaEngine.GenerationResult.Complete? = null

            llamaEngine.generate(prompt, config).collect { result ->
                when (result) {
                    is LlamaEngine.GenerationResult.Token -> {
                        fullResponse += result.token
                        emit(NarrativeResult.StreamingToken(result.token))
                    }
                    is LlamaEngine.GenerationResult.Complete -> {
                        fullResponse = result.fullText
                        lastMetrics = result
                        emit(NarrativeResult.GenerationComplete(result))
                    }
                    is LlamaEngine.GenerationResult.Error -> {
                        emit(NarrativeResult.Failed(result.message, 0))
                        return@collect
                    }
                }
            }

            // 3. Valida JSON
            val validationResult = validator.validate(fullResponse)
            
            if (validationResult.isSuccess) {
                val contract = validationResult.getOrThrow()
                emit(
                    NarrativeResult.Success(
                        contract = contract,
                        metrics = lastMetrics
                    )
                )
            } else {
                // 4. Fallback: narrativa simples determinística
                val fallbackContract = createFallbackNarrative(facts, tone)
                emit(
                    NarrativeResult.SuccessWithFallback(
                        contract = fallbackContract,
                        originalError = validationResult.exceptionOrNull()?.message ?: "Validação falhou"
                    )
                )
            }
        } catch (e: Exception) {
            emit(NarrativeResult.Failed(e.message ?: "Erro desconhecido", 0))
        }
    }

    /**
     * Gera narrativa de teste (para Fase 0).
     */
    fun generateTest(
        template: ChatTemplate,
        config: LlamaEngine.ModelConfig = LlamaEngine.ModelConfig()
    ): Flow<NarrativeResult> = flow {
        val prompt = promptBuilder.buildTestPrompt(template)
        emit(NarrativeResult.PromptBuilt(prompt))

        var fullResponse = ""
        var lastMetrics: LlamaEngine.GenerationResult.Complete? = null

        llamaEngine.generate(prompt, config).collect { result ->
            when (result) {
                is LlamaEngine.GenerationResult.Token -> {
                    fullResponse += result.token
                    emit(NarrativeResult.StreamingToken(result.token))
                }
                is LlamaEngine.GenerationResult.Complete -> {
                    fullResponse = result.fullText
                    lastMetrics = result
                    emit(NarrativeResult.GenerationComplete(result))
                }
                is LlamaEngine.GenerationResult.Error -> {
                    emit(NarrativeResult.Failed(result.message, 0))
                    return@collect
                }
            }
        }

        val validationResult = validator.validate(fullResponse)
        
        if (validationResult.isSuccess) {
            val contract = validationResult.getOrThrow()
            emit(
                NarrativeResult.Success(
                    contract = contract,
                    metrics = lastMetrics
                )
            )
        } else {
            val fallbackContract = createFallbackNarrative(
                facts = listOf("Teste de taverna"),
                tone = NarrativeTone.tensao
            )
            emit(
                NarrativeResult.SuccessWithFallback(
                    contract = fallbackContract,
                    originalError = validationResult.exceptionOrNull()?.message ?: "Validação falhou"
                )
            )
        }
    }

    /**
     * Cria narrativa determinística simples como fallback.
     */
    private fun createFallbackNarrative(
        facts: List<String>,
        tone: NarrativeTone
    ): NarratorContract {
        val simpleNarrative = facts.joinToString(" ") + " A cena continua."
        
        return NarratorContract(
            contract_version = "1.0",
            response_type = ResponseType.TURN_NARRATION,
            narrative = simpleNarrative,
            sensory_focus = listOf("visao"),
            npcs_mentioned = emptyList(),
            tone = tone,
            warnings = listOf("Narrativa de fallback (LLM falhou)"),
            error = null
        )
    }

    sealed class NarrativeResult {
        data class PromptBuilt(val prompt: String) : NarrativeResult()
        data class StreamingToken(val token: String) : NarrativeResult()
        data class GenerationComplete(val metrics: LlamaEngine.GenerationResult.Complete) : NarrativeResult()
        data class Success(
            val contract: NarratorContract,
            val metrics: LlamaEngine.GenerationResult.Complete?
        ) : NarrativeResult()
        data class SuccessWithFallback(
            val contract: NarratorContract,
            val originalError: String
        ) : NarrativeResult()
        data class Failed(val error: String, val attemptNumber: Int) : NarrativeResult()
    }
}

package com.mundovivo.llm

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Contrato JSON que o LLM deve retornar (forçado por GBNF).
 */
@Serializable
data class NarratorContract(
    val contract_version: String = "1.0",
    val response_type: ResponseType,
    val narrative: String,
    val sensory_focus: List<String>,
    val npcs_mentioned: List<String>,
    val tone: NarrativeTone,
    val warnings: List<String>,
    val error: String?
)

@Serializable
enum class ResponseType {
    TURN_NARRATION,
    ERROR,
    SYSTEM_MESSAGE
}

@Serializable
enum class NarrativeTone {
    terror,
    humor,
    sensualidade,
    melancolia,
    acao,
    contemplativo,
    cotidiano,
    tensao,
    maravilhamento
}

/**
 * Validador do contrato do narrador.
 */
class NarratorContractValidator {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    /**
     * Valida e parseia a resposta do LLM.
     */
    fun validate(rawJson: String): Result<NarratorContract> {
        return try {
            val contract = json.decodeFromString<NarratorContract>(rawJson)
            
            // Validações adicionais
            val errors = mutableListOf<String>()

            if (contract.narrative.isBlank()) {
                errors.add("Narrativa vazia")
            }

            if (contract.narrative.length > 2000) {
                errors.add("Narrativa muito longa (>${contract.narrative.length} chars)")
            }

            // Verifica se LLM não narrou ações do jogador (violação de agência)
            val agencyViolations = detectAgencyViolations(contract.narrative)
            if (agencyViolations.isNotEmpty()) {
                errors.add("Violação de agência detectada: $agencyViolations")
            }

            if (errors.isNotEmpty()) {
                Result.failure(ValidationException(errors.joinToString("; ")))
            } else {
                Result.success(contract)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Detecta frases que violam agência do jogador.
     *
     * IMPORTANTE: usamos apenas padrões de segunda pessoa direta ("você...")
     * que inequivocamente se dirigem ao jogador. Evitamos "sua mente"/"seu
     * coração" porque geram falso-positivo quando o narrador se refere a
     * NPCs em terceira pessoa (ex.: "Torvin apertava suas mãos, sua mente
     * fervia" — legítimo).
     */
    private fun detectAgencyViolations(narrative: String): List<String> {
        val violations = mutableListOf<String>()
        val lowerNarrative = narrative.lowercase()

        val forbiddenPatterns = listOf(
            "você sente",
            "você pensa",
            "você decide",
            "você percebe que",
            "você se lembra de",
            "você acredita",
            "você entende que"
        )

        forbiddenPatterns.forEach { pattern ->
            if (lowerNarrative.contains(pattern)) {
                violations.add(pattern)
            }
        }

        return violations
    }
}

class ValidationException(message: String) : Exception(message)

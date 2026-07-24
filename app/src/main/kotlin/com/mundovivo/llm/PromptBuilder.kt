package com.mundovivo.llm

/**
 * Constrói prompts para o narrador LLM.
 * 
 * Responsável por:
 * - Montar system prompt com WorldRules e AgencyRules
 * - Transformar fatos canônicos em user prompt
 * - Aplicar template de chat (ChatML/Gemma)
 * - Truncar se necessário
 */
class PromptBuilder {

    companion object {
        private const val MAX_CONTEXT_TOKENS = 2048
        private const val APPROX_CHARS_PER_TOKEN = 4
        private const val MAX_PROMPT_CHARS = MAX_CONTEXT_TOKENS * APPROX_CHARS_PER_TOKEN
    }

    /**
     * System prompt base do narrador.
     */
    private fun buildSystemPrompt(tone: NarrativeTone): String {
        return """
            Você é o narrador de um RPG procedural em português brasileiro.
            
            REGRAS CRÍTICAS (NUNCA VIOLE):
            1. AGÊNCIA: Você NUNCA narra ações, pensamentos ou sentimentos do jogador.
               - ❌ Proibido: "você sente", "você pensa", "você decide", "você percebe que"
               - ✅ Permitido: descrever ambiente, NPCs, eventos externos
            
            2. FATOS CANÔNICOS: Você só narra os fatos fornecidos.
               - Não invente ações, itens, NPCs ou eventos não autorizados.
               - Se um fato diz "porta trancada", não diga "você consegue abrir".
            
            3. FORMATO: Responda SEMPRE em JSON válido seguindo o contrato exato.
            
            4. TOM: Use o tom "${tone.name}" na narrativa.
            
            5. SENSORIAL: Priorize detalhes sensoriais (visão, audição, olfato, tato).
            
            CONTRATO JSON OBRIGATÓRIO:
            {
              "contract_version": "1.0",
              "response_type": "TURN_NARRATION",
              "narrative": "sua narrativa aqui",
              "sensory_focus": ["visao", "audicao"],
              "npcs_mentioned": ["Nome do NPC"],
              "tone": "${tone.name}",
              "warnings": [],
              "error": null
            }
        """.trimIndent()
    }

    /**
     * Constrói user prompt a partir de fatos canônicos.
     */
    private fun buildUserPrompt(facts: List<String>, context: String?): String {
        return buildString {
            if (context != null) {
                append("CONTEXTO:\n")
                append(context)
                append("\n\n")
            }

            append("FATOS CANÔNICOS:\n")
            facts.forEachIndexed { index, fact ->
                append("${index + 1}. $fact\n")
            }

            append("\n")
            append("Narre esses fatos de forma atmosférica, sem adicionar eventos não autorizados.")
        }
    }

    /**
     * Constrói prompt completo para o narrador.
     */
    fun buildNarratorPrompt(
        facts: List<String>,
        tone: NarrativeTone,
        template: ChatTemplate,
        context: String? = null
    ): String {
        val systemPrompt = buildSystemPrompt(tone)
        val userPrompt = buildUserPrompt(facts, context)

        val fullPrompt = ChatMLFormatter.format(template, systemPrompt, userPrompt)

        // Trunca se muito longo
        return if (fullPrompt.length > MAX_PROMPT_CHARS) {
            val truncatedFacts = facts.take(facts.size / 2) // Remove metade dos fatos
            val truncatedUserPrompt = buildUserPrompt(truncatedFacts, null) // Remove contexto
            ChatMLFormatter.format(template, systemPrompt, truncatedUserPrompt)
        } else {
            fullPrompt
        }
    }

    /**
     * Constrói prompt de teste simples (para Fase 0).
     */
    fun buildTestPrompt(template: ChatTemplate): String {
        val systemPrompt = buildSystemPrompt(NarrativeTone.tensao)
        val userPrompt = buildUserPrompt(
            facts = listOf(
                "Local: Taverna do Corvo Partido, noite chuvosa.",
                "O jogador pediu uma bebida ao taverneiro.",
                "O taverneiro serviu uma cerveja escura.",
                "Torvin está sentado no canto da taverna.",
                "Torvin observou discretamente o jogador."
            ),
            context = "Primeira cena do jogo. Tom: tenso, atmosférico."
        )

        return ChatMLFormatter.format(template, systemPrompt, userPrompt)
    }
}

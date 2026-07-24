package com.mundovivo.llm

/**
 * Formatadores de prompt para diferentes templates de chat.
 */
object ChatMLFormatter {

    /**
     * Formata prompt usando ChatML (Qwen).
     * 
     * Template:
     * <|im_start|>system
     * {system_prompt}
     * <|im_end|>
     * <|im_start|>user
     * {user_prompt}
     * <|im_end|>
     * <|im_start|>assistant
     */
    fun formatChatML(systemPrompt: String, userPrompt: String): String {
        return buildString {
            append("<|im_start|>system\n")
            append(systemPrompt)
            append("\n<|im_end|>\n")
            append("<|im_start|>user\n")
            append(userPrompt)
            append("\n<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
    }

    /**
     * Formata prompt usando Gemma template.
     * 
     * Template:
     * <start_of_turn>user
     * {prompt}
     * <end_of_turn>
     * <start_of_turn>model
     */
    fun formatGemma(systemPrompt: String, userPrompt: String): String {
        return buildString {
            append("<start_of_turn>user\n")
            append("$systemPrompt\n\n$userPrompt")
            append("\n<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
    }

    /**
     * Formata automaticamente baseado no template do modelo.
     */
    fun format(template: ChatTemplate, systemPrompt: String, userPrompt: String): String {
        return when (template) {
            ChatTemplate.CHATML -> formatChatML(systemPrompt, userPrompt)
            ChatTemplate.GEMMA -> formatGemma(systemPrompt, userPrompt)
            ChatTemplate.LLAMA3 -> formatLlama3(systemPrompt, userPrompt)
            ChatTemplate.ALPACA -> formatAlpaca(systemPrompt, userPrompt)
        }
    }

    private fun formatLlama3(systemPrompt: String, userPrompt: String): String {
        return buildString {
            append("<|start_header_id|>system<|end_header_id|>\n")
            append(systemPrompt)
            append("\n<|eot_id|>")
            append("<|start_header_id|>user<|end_header_id|>\n")
            append(userPrompt)
            append("\n<|eot_id|>")
            append("<|start_header_id|>assistant<|end_header_id|>\n")
        }
    }

    private fun formatAlpaca(systemPrompt: String, userPrompt: String): String {
        return buildString {
            append("### Instruction:\n")
            append("$systemPrompt\n\n$userPrompt")
            append("\n\n### Response:\n")
        }
    }
}

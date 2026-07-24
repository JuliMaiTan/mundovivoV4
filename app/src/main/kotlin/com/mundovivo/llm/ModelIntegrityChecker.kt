package com.mundovivo.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Verifica integridade de modelos via SHA256.
 */
class ModelIntegrityChecker {

    /**
     * Verifica se o arquivo tem o checksum esperado.
     * 
     * @return Result.success(true) se válido, Result.failure se inválido ou erro
     */
    suspend fun verify(file: File, expectedSha256: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Arquivo não existe: ${file.path}"))
            }

            val actualSha256 = calculateSha256(file)
            val isValid = actualSha256.equals(expectedSha256, ignoreCase = true)

            if (isValid) {
                Result.success(true)
            } else {
                Result.failure(
                    SecurityException(
                        "Checksum inválido. Esperado: $expectedSha256, Obtido: $actualSha256"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calcula SHA256 de um arquivo.
     */
    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

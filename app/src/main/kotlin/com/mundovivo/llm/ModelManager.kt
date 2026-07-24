package com.mundovivo.llm

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Gerenciador central de modelos LLM.
 * 
 * Responsável por:
 * - Download de modelos
 * - Importação de modelos externos
 * - Verificação de integridade
 * - Status de modelos
 * - Modelo ativo
 */
class ModelManager(
    private val context: Context,
    private val downloader: ModelDownloader = ModelDownloader(),
    private val integrityChecker: ModelIntegrityChecker = ModelIntegrityChecker()
) {

    private val modelsDir: File = File(context.filesDir, "models").apply { mkdirs() }

    /**
     * Diretório onde os modelos são armazenados.
     */
    fun getModelsDirectory(): File = modelsDir

    /**
     * Retorna o arquivo local para um modelo.
     */
    fun getModelFile(modelId: ModelId): File {
        return File(modelsDir, "${modelId.value}.gguf")
    }

    /**
     * Verifica status de um modelo.
     *
     * Nota: `sizeBytes` no ModelCatalog é aproximado (~). Por isso NÃO usamos
     * comparação estrita de tamanho — se o arquivo existe, entregamos para o
     * IntegrityChecker (ou log de warning se SHA256 for placeholder).
     * A verdade final é o SHA256; o tamanho serve só para UI de progresso.
     */
    suspend fun getModelStatus(modelId: ModelId): ModelStatus = withContext(Dispatchers.IO) {
        val file = getModelFile(modelId)
        val model = ModelCatalog.findById(modelId) ?: return@withContext ModelStatus.ERROR

        when {
            !file.exists() -> ModelStatus.NOT_DOWNLOADED
            // Só considera DOWNLOADING se estiver bem abaixo (< 90%) do tamanho declarado,
            // para evitar falso-negativo quando o tamanho real difere ligeiramente do estimado.
            file.length() < (model.sizeBytes * 0.9).toLong() -> ModelStatus.DOWNLOADING
            else -> {
                // Verifica integridade (skip apenas em dev quando SHA256 é placeholder)
                if (model.sha256.startsWith("PLACEHOLDER_")) {
                    android.util.Log.w(
                        "ModelManager",
                        "⚠️ SHA256 é PLACEHOLDER para ${modelId.value}. " +
                        "Validação de integridade DESATIVADA. " +
                        "Substitua o hash real em ModelCatalog antes do release."
                    )
                    ModelStatus.READY // Dev only
                } else {
                    val isValid = integrityChecker.verify(file, model.sha256).getOrNull() ?: false
                    if (isValid) ModelStatus.READY else ModelStatus.CORRUPTED
                }
            }
        }
    }

    /**
     * Faz download de um modelo.
     * 
     * @return Flow com progresso de download
     */
    fun downloadModel(modelInfo: ModelInfo): Flow<DownloadProgress> {
        val destination = getModelFile(modelInfo.id)
        return downloader.download(modelInfo, destination)
    }

    /**
     * Importa modelo de URI externa (ex: usuário selecionou arquivo).
     */
    suspend fun importExternalModel(uri: Uri, modelId: ModelId): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destination = getModelFile(modelId)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalArgumentException("Não foi possível abrir URI"))

            Result.success(destination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove modelo do dispositivo.
     */
    suspend fun deleteModel(modelId: ModelId): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = getModelFile(modelId)
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lista modelos baixados.
     */
    suspend fun getDownloadedModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        ModelCatalog.getSupportedModels().filter { model ->
            getModelFile(model.id).exists()
        }
    }
}

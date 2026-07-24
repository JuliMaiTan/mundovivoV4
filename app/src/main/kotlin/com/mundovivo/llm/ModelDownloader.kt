package com.mundovivo.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Download resumível de modelos usando OkHttp.
 *
 * Correções:
 * - Usa FileOutputStream(destination, append=true) para NÃO truncar em resume
 * - Usa channelFlow + flowOn(Dispatchers.IO) para respeitar contexto do Flow
 * - Trata resposta 206 (Partial Content) e 200 (Full) corretamente
 */
class ModelDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Faz download de um modelo com suporte real a resume.
     *
     * @param modelInfo Modelo a baixar
     * @param destination Arquivo destino
     * @return Flow com progresso de download
     */
    fun download(
        modelInfo: ModelInfo,
        destination: File
    ): Flow<DownloadProgress> = channelFlow {
        // Cria pasta pai se não existir
        destination.parentFile?.mkdirs()

        // Se arquivo já existe, tenta resume
        val startByte = if (destination.exists()) destination.length() else 0L

        val request = Request.Builder()
            .url(modelInfo.downloadUrl)
            .apply {
                if (startByte > 0) {
                    addHeader("Range", "bytes=$startByte-")
                }
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download falhou: ${response.code} ${response.message}")
            }

            val body = response.body ?: throw IOException("Body vazio")
            val contentLength = body.contentLength()

            // Determina total real de bytes:
            // - Se status 206 (Partial), contentLength = restante; total = startByte + contentLength
            // - Se status 200 (Full), servidor ignorou o Range; total = contentLength (arquivo será reescrito do zero)
            val isPartialResponse = response.code == 206
            val effectiveStartByte = if (isPartialResponse) startByte else 0L
            val totalBytes = if (isPartialResponse) {
                startByte + contentLength
            } else {
                contentLength
            }

            if (totalBytes <= 0) {
                throw IOException("Tamanho do arquivo desconhecido")
            }

            // Abre arquivo com append=true SE for resume real (206).
            // Caso contrário reescreve do zero (200 = servidor não suporta Range).
            val outputStream = FileOutputStream(destination, isPartialResponse)

            outputStream.use { output ->
                val buffer = ByteArray(8192)
                var downloadedBytes = effectiveStartByte
                var lastEmitTime = System.currentTimeMillis()
                val startTime = System.currentTimeMillis()

                body.byteStream().use { input ->
                    var bytesRead = input.read(buffer)
                    while (bytesRead != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Emite progresso a cada 500ms
                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime >= 500) {
                            val elapsedSeconds = (now - startTime) / 1000.0
                            val speed = if (elapsedSeconds > 0) {
                                (downloadedBytes - effectiveStartByte) / elapsedSeconds
                            } else {
                                0.0
                            }

                            send(
                                DownloadProgress(
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    speedBytesPerSecond = speed.toLong()
                                )
                            )
                            lastEmitTime = now
                        }

                        bytesRead = input.read(buffer)
                    }
                }

                // Emite progresso final (100%)
                val totalElapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                val avgSpeed = if (totalElapsedSeconds > 0) {
                    (downloadedBytes - effectiveStartByte) / totalElapsedSeconds
                } else {
                    0.0
                }

                send(
                    DownloadProgress(
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        speedBytesPerSecond = avgSpeed.toLong()
                    )
                )
            }
        }
    }.flowOn(Dispatchers.IO)
}

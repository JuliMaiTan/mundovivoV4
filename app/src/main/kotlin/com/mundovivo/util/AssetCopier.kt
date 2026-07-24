package com.mundovivo.util

import android.content.Context
import com.mundovivo.BuildConfig
import java.io.File

/**
 * Utilitário para copiar arquivos de `assets/` para `filesDir/` no primeiro uso
 * (ou quando a versão do app muda).
 *
 * Necessário porque componentes nativos (llama.cpp/JNI) precisam de um caminho
 * de arquivo real do filesystem — assets não são acessíveis via path direto.
 *
 * Versionamento por `BuildConfig.VERSION_CODE`: cada release cria um subdiretório
 * novo (ex: `filesDir/grammar/v42/`), evitando servir asset stale após upgrade
 * do APK.
 */
object AssetCopier {

    /**
     * Copia um asset para filesDir/<subdir>/v<versionCode>/<filename> se ainda
     * não existir. Versões antigas ficam no disco (podem ser limpas via
     * `cleanStaleVersions`).
     *
     * @param context Contexto Android
     * @param assetPath Caminho do asset (ex: "grammar/narrator_contract_v1.gbnf")
     * @param destSubdir Subdiretório dentro de filesDir (ex: "grammar")
     * @return File pronto para uso pelo native lib
     */
    fun copyAssetToFilesDir(
        context: Context,
        assetPath: String,
        destSubdir: String
    ): File {
        val fileName = assetPath.substringAfterLast('/')
        val versionedDir = File(context.filesDir, "$destSubdir/v${BuildConfig.VERSION_CODE}")
            .apply { mkdirs() }
        val destFile = File(versionedDir, fileName)

        if (!destFile.exists()) {
            context.assets.open(assetPath).use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return destFile
    }

    /**
     * Retorna o arquivo da grammar GBNF do narrador.
     */
    fun getNarratorGrammarFile(context: Context): File {
        return copyAssetToFilesDir(
            context = context,
            assetPath = "grammar/narrator_contract_v1.gbnf",
            destSubdir = "grammar"
        )
    }

    /**
     * Remove versões antigas de assets (mantém apenas a versão atual).
     * Chamar opcionalmente em background após app abrir.
     */
    fun cleanStaleVersions(context: Context, subdir: String) {
        val root = File(context.filesDir, subdir)
        if (!root.exists() || !root.isDirectory) return

        val currentVersionDir = "v${BuildConfig.VERSION_CODE}"
        root.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name.startsWith("v") && dir.name != currentVersionDir) {
                dir.deleteRecursively()
            }
        }
    }
}

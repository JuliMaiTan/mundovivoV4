# Status do Projeto - Mundo Vivo V4

## Estado real

Fase 0 ainda e bootstrap. O repo tem um scaffold Android consistente e agora possui build Android validado com LlamaEngine mockado. O app ainda nao foi validado como produto funcional com llama.cpp real.

O que existe hoje:

- Projeto Android Kotlin/Compose em Gradle KTS.
- UI de selecao, download e teste de modelo.
- Camada LLM com catalogo, downloader, checksum, prompt builder e contrato JSON.
- GBNF versionado em assets.
- Qwen com SHA256 real.
- LlamaEngine mockado para validar UI sem native lib.
- Gradle wrapper completo versionado.
- `assembleDebug` passando com mock.
- Testes unitarios passando.

O que ainda nao existe:

- JNI real para llama.cpp.
- Submodulo llama.cpp.
- CMake funcional para inferencia.
- Gemma com SHA256 real.
- APK validado em celular.
- Motor canonico/Room/TurnProcessor.

## Progresso

```text
Scaffold + docs: 100%
Build com mock: 100%
llama.cpp real: 0%
Device real: 0%
Motor canonico: 0%
```

Estimativa honesta da Fase 0: 50-60%. A arquitetura e o build com mock estao prontos; a execucao real ainda depende de llama.cpp, JNI, GBNF nativo e device.

## Concluido

### Estrutura Android

- `settings.gradle.kts`, `build.gradle.kts` root e app.
- `gradlew`, `gradlew.bat` e `gradle-wrapper.jar`.
- Plugin `kotlin.plugin.serialization`.
- Compose + Material 3.
- Navigation Compose.
- ProGuard rules.
- `local.properties.example`.

### Modelos

- `ModelCatalog.kt` com Qwen e Gemma.
- Qwen SHA256 real: `e4810db9a69da2d070883eaade92a85456b63720407afdc0c2a7b9155613866b`.
- Gemma ainda placeholder.
- `ModelDownloader.kt` com resume e tratamento 200/206.
- `ModelIntegrityChecker.kt`.
- `ModelManager.kt`.

### Narrador

- `narrator_contract_v1.gbnf`.
- `NarratorContract.kt`.
- `PromptBuilder.kt`.
- `ChatMLFormatter.kt`.
- `NarrativeGenerator.kt`.
- `AssetCopier.kt` para copiar GBNF para `filesDir`.

### UI

- `ModelSelectionScreen.kt`.
- `ModelDownloadScreen.kt`.
- `LLMTestScreen.kt`.
- `NavGraph.kt`.

## Validacao executada

- `./gradlew.bat assembleDebug` passou.
- `./gradlew.bat test` passou.
- APK debug gerado em `app/build/outputs/apk/debug/app-debug.apk`.

Warnings conhecidos:

- `LlamaEngine` ainda tem parametros nao usados porque esta mockado.
- `LLMTestScreen.onBack` ainda nao e usado.
- `android.defaults.buildfeatures.buildconfig=true` esta deprecado e deve migrar depois.

## Bloqueadores atuais

1. llama.cpp ainda nao esta integrado.
2. JNI ainda nao esta implementado.
3. Grammar GBNF ainda nao roda no llama.cpp real.
4. APK ainda nao foi instalado/testado em celular.

## Proximo checkpoint

### P0 - Device com mock

- Instalar `app-debug.apk` via adb.
- Validar navegacao: selecao de modelo -> download -> teste taverna.
- Confirmar que a UI abre sem crash.

### P0 - llama.cpp real

- Adicionar submodulo llama.cpp pinado.
- Implementar JNI minimo sem streaming.
- Depois adicionar streaming, grammar, cancelamento e metricas.

## Notas de arquitetura

- LLM nunca altera o mundo.
- Motor canonico decide a verdade.
- Narrador recebe fatos autorizados e devolve JSON.
- GBNF e primeira camada de controle; validador Kotlin e segunda.
- Evitar liberar modelo sempre em `onPause`; preferir `onTrimMemory`, timeout em background ou acao explicita.

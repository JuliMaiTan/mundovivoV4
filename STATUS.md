# Status do Projeto - Mundo Vivo 2.0

## ⚠️ Status Realista

**Fase 0 = Bootstrap arquitetural / documental PRONTO.**
**Fase 0 = App validado como buildável e rodando em celular: AINDA NÃO.**

Não confundir "arquivos criados" com "app funcional". O que existe é:
- Scaffold Kotlin completo com API contracts claros
- Documentação de arquitetura + guia de integração llama.cpp
- Mock de LlamaEngine que faz UI/navegação funcionarem sem native lib
- Ainda não rodou `./gradlew assembleDebug` nem foi testado em device

## ✅ Concluído

### Estrutura Base Android
- [x] `settings.gradle.kts`, `build.gradle.kts` (root + app)
- [x] Plugin `kotlin.plugin.serialization` aplicado
- [x] `gradle.properties`
- [x] AndroidManifest.xml
- [x] ProGuard rules
- [x] `.gitignore`
- [x] Gradle wrapper properties

### Utilidades Core
- [x] `DeviceInfo.kt`
- [x] `RamFormatter.kt`
- [x] `Ids.kt`
- [x] `Time.kt`

### Sistema de Modelos LLM
- [x] `ModelTypes.kt`
- [x] `ModelCatalog.kt` (SHA256 ainda placeholder — ver TODOs)
- [x] `ModelSelectionPolicy.kt`
- [x] `ModelIntegrityChecker.kt`
- [x] `ModelDownloader.kt` (resume real via `FileOutputStream(append=true)` + `channelFlow` + `flowOn(IO)`)
- [x] `ModelManager.kt` (log de warning quando pula SHA256)

### Contrato LLM
- [x] `narrator_contract_v1.gbnf`
- [x] `NarratorContract.kt` (usa `@Serializable`, plugin já configurado)

### UI Base
- [x] `MundoVivoApp.kt`
- [x] `MainActivity.kt` (usa `MundoVivoNavGraph`)
- [x] `Theme.kt` + `Type.kt`
- [x] `ModelSelectionScreen.kt`
- [x] `ModelDownloadScreen.kt`
- [x] `LLMTestScreen.kt`
- [x] `NavGraph.kt` (com `MundoVivoNavGraph`)

### LLM Layer (mock funcional)
- [x] `LlamaEngine.kt` (⚠️ MOCK — retorna JSON fake para testes de UI)
- [x] `LlamaNativeBridge.kt` (stub — `initModel/generate/freeModel` declarados external)
- [x] `ChatMLFormatter.kt` (ChatML, Gemma, Llama3, Alpaca)
- [x] `PromptBuilder.kt` (system prompt com AgencyRules)
- [x] `NarrativeGenerator.kt` (orquestração + fallback determinístico)

### Native (Preparado, NÃO compila ainda)
- [x] `CMakeLists.txt` (esqueleto, `add_subdirectory(llama.cpp)` comentado)
- [x] `llama-jni.cpp` (stub sem implementação)

### Documentação
- [x] `README.md`
- [x] `ARCHITECTURE.md`
- [x] `STATUS.md`
- [x] `LLAMA_INTEGRATION.md` (guia detalhado)
- [x] `memory/PRD.md`

## 🐛 Bugs Corrigidos Nesta Iteração

1. ✅ **Plugin serialization ausente**: `kotlin.plugin.serialization` adicionado no root e app `build.gradle.kts`.
2. ✅ **ModelDownloader resume trunca**: Trocado para `FileOutputStream(destination, append=true)` quando resposta é 206 Partial Content.
3. ✅ **Flow context violation**: `ModelDownloader` agora usa `channelFlow { send(...) }.flowOn(Dispatchers.IO)` em vez de emitir dentro de `withContext`.
4. ✅ **SHA256 placeholder silencioso**: `ModelManager` agora emite `Log.w` claro quando pula validação; comentários em `ModelCatalog` documentam como calcular o hash real.
5. ✅ **Range response 200 vs 206**: `ModelDownloader` trata corretamente o caso em que o servidor ignora o header `Range` (status 200 = reescreve do zero, sem duplicar bytes).
6. ✅ **Grammar GBNF não era passada**: `LLMTestScreen` agora usa `AssetCopier.getNarratorGrammarFile(context)` e passa para `loadModel(modelFile, grammarFile)`. Constrained decoding vai funcionar quando o JNI real entrar.
7. ✅ **freeModel() async em onCleared**: `LlamaEngine.freeModel()` agora é **não-suspend / síncrono**. `LLMTestViewModel.onCleared()` chama direto sem depender do `viewModelScope` (que já pode estar cancelado).
8. ✅ **SHA256 real do Qwen**: `e4810db9a69da2d070883eaade92a85456b63720407afdc0c2a7b9155613866b` — validação de integridade REAL agora acontece para Qwen (só Gemma continua com placeholder).
9. ✅ **AssetCopier stale em upgrade**: agora versiona por `BuildConfig.VERSION_CODE` (`filesDir/grammar/v<N>/narrator_contract_v1.gbnf`) e tem helper `cleanStaleVersions()` para limpar versões antigas.

## 🚧 Ainda Faltando para Fase 0 Realmente Funcional

### P0 - Ambiente de Build (parcialmente feito)
- [x] JDK 17 (ARM64) instalado no terminal remoto
- [x] Gradle 8.2 funcional
- [ ] **Android SDK** ← bloqueador atual (script pronto em `/app/scripts/setup-android-sdk.sh`)
- [ ] `/app/local.properties` com `sdk.dir=...` (gerado pelo script)
- [ ] Gradle wrapper (`./gradlew`, `gradle-wrapper.jar`) gerado localmente

### P0 - Build Fix (depende do SDK)
- [ ] Rodar `./gradlew assembleDebug` e resolver o que quebrar
- [ ] Gerar APK debug em `/app/app/build/outputs/apk/debug/app-debug.apk`
- [ ] Instalar em celular via `adb install app-debug.apk`
- [ ] Navegar telas com mock e confirmar que UI abre

### P0 - Integração llama.cpp Real (guia em LLAMA_INTEGRATION.md)
- [ ] `git submodule add https://github.com/ggerganov/llama.cpp cpp/llama.cpp`
- [ ] Completar `CMakeLists.txt` (descomentar `add_subdirectory(llama.cpp)` + link libs)
- [ ] Implementar `llama-jni.cpp` (initModel, generate com callback JNI, freeModel)
- [ ] Descomentar chamadas JNI em `LlamaEngine.kt` e remover mock
- [ ] Build native com NDK 25+
- [ ] Baixar Qwen 1.5B Q4_K_M
- [ ] Calcular SHA256 real e substituir placeholder em `ModelCatalog`
- [ ] Testar em celular físico

### P0 - Validação Fase 0 (aceite formal)
- [ ] Prompt taverna gera JSON válido via GBNF
- [ ] `sensory_focus`, `npcs_mentioned`, `warnings` como arrays
- [ ] Campo `error` presente (não "erro")
- [ ] `tone` no enum
- [ ] Métricas exibidas: tok/s, TTFT, RAM
- [ ] Rodou em Edge 20 Pro (ou similar 4GB+ ARM64)

## 🔄 Próximas Fases

Ver `memory/PRD.md` para backlog completo (Fases 1-5).

## 📊 Progresso Fase 0

```
Scaffold + Docs:  ████████████████████ 100%
Build validado:   ░░░░░░░░░░░░░░░░░░░░   0%
llama.cpp real:   ░░░░░░░░░░░░░░░░░░░░   0%
Device testado:   ░░░░░░░░░░░░░░░░░░░░   0%
```

**Overall Fase 0**: ~40% (scaffold OK, execução real ainda a fazer).

## 🎯 Foco Imediato

1. **Build local** — rodar Gradle assembleDebug e caçar o que quebra
2. **Integrar llama.cpp** — seguir `LLAMA_INTEGRATION.md`
3. **Rodar em celular** — validar métricas reais

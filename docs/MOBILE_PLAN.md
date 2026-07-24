# Mundo Vivo V4 - Plano Mobile Revisado

## Visao

Mundo Vivo V4 e um RPG narrativo procedural offline-first para Android. O app usa um motor canonico deterministico como fonte da verdade e um LLM local via llama.cpp apenas para narrar fatos ja autorizados.

Regra de ouro: se muda o jogo, pertence ao motor. Se apenas descreve, pode ir para o narrador.

## Requisitos inegociaveis

- Android nativo com Kotlin e Jetpack Compose.
- Execucao offline no device.
- Meta comercial: rodar em aparelhos 4GB RAM com Qwen 1.5B, com validacao obrigatoria em device fisico.
- Gemma 2B como modo qualidade para 8GB+ RAM.
- Modelo baixado separadamente do APK, com resume e SHA256.
- GBNF obrigatorio para restringir JSON na geracao.
- Validador Kotlin como segunda camada.
- Streaming de tokens visivel na UI.
- LLM nunca escreve estado canonico.

## Stack

- Kotlin 1.9+ para app.
- C++17 para JNI.
- Jetpack Compose + Material 3.
- Coroutines + Flow.
- Room/SQLite a partir da Fase 1.
- llama.cpp via JNI.
- Gradle KTS + CMake.
- arm64-v8a como ABI principal para inferencia real.

## Modelos

| Modelo | Uso | Perfil |
| --- | --- | --- |
| Qwen 2.5 1.5B abliterated Q4_K_M | Default 4-6GB | Rapido e leve; PT-BR aceitavel; precisa GBNF |
| Gemma 2 2B abliterated Q4_K_M | Qualidade 8GB+ | Narrativa mais densa; maior RAM e TTFT |

Politica de UX:

- 4GB: recomendar Qwen e bloquear Gemma com aviso.
- 6GB: recomendar Qwen, permitir Gemma com warning.
- 8GB+: permitir ambos e sugerir benchmark curto.
- Na UI, mostrar nomes amigaveis: "Rapido e leve" e "Narrativa mais densa".

## Arquitetura

```text
Player input
  -> TurnProcessor
  -> Canonical Engine
       -> ActionResolver
       -> WorldState
       -> AgencyRules
       -> Events
  -> Authorized facts
  -> PromptBuilder
  -> llama.cpp + GBNF
  -> NarratorContractValidator
  -> UI streaming
  -> Persist turn/replay
```

O LLM recebe fatos canonicos e devolve prosa em JSON. O motor decide sucesso, falha, consequencias, NPCs, inventario, relacoes e estado persistente.

## Estado real do repo

Ja existe:

- Scaffold Android em Gradle KTS.
- UI de selecao/download/teste de modelo.
- ModelManager, ModelDownloader, ModelIntegrityChecker e catalogo Qwen/Gemma.
- PromptBuilder, ChatMLFormatter, NarrativeGenerator e contrato JSON.
- GBNF em `app/src/main/assets/grammar/narrator_contract_v1.gbnf`.
- SHA256 real do Qwen.
- Documentacao de arquitetura e integracao llama.cpp.

Ainda mock ou pendente:

- `LlamaEngine.kt` gera JSON fake.
- `llama-jni.cpp` nao implementa JNI real.
- `CMakeLists.txt` ainda nao compila llama.cpp.
- Submodulo llama.cpp ainda nao existe.
- Gemma ainda tem SHA256 placeholder.
- Nenhum APK validado em celular.

## Fase 0 - Bootstrap LLM real

### Bloco 1 - Ambiente e build com mock

- Versionar Gradle wrapper completo: `gradlew`, `gradlew.bat`, `gradle-wrapper.jar`.
- Configurar Android SDK local/remoto.
- Rodar `./gradlew assembleDebug` com `LlamaEngine` mock.
- Instalar APK no celular e validar navegacao basica.

Status atual: wrapper completo gerado, `assembleDebug` passou e testes unitarios passaram. Falta instalar o APK no device.

### Bloco 2 - llama.cpp

- Adicionar llama.cpp como submodulo pinado em tag especifica.
- Configurar CMake para arm64-v8a.
- Manter ABI inicial restrita para reduzir complexidade.

### Bloco 3 - JNI minimo

Implementar primeiro sem streaming:

- `initModel`.
- `generate` sincrono.
- `freeModel`.
- `ModelState` via `jlong`.

Depois adicionar:

- streaming por callback Kotlin;
- cancelamento;
- tratamento de lifecycle;
- metricas TTFT/tok/s;
- grammar.

### Bloco 4 - GBNF real

- Usar grammar API da tag pinada do llama.cpp.
- Passar o arquivo copiado por `AssetCopier`.
- Validar que `sensory_focus`, `npcs_mentioned`, `warnings` sao arrays e `error` existe.

### Bloco 5 - Validacao em device

DoD:

- App abre sem PocketPal.
- Qwen baixa com resume e passa SHA256.
- Prompt taverna retorna JSON valido via GBNF.
- Streaming visivel.
- 10 turnos consecutivos sem crash.
- Metas Qwen: >=15 tok/s, TTFT <2500ms, RAM PSS <2GB.

## Fase 1 - Motor canonico

- Room schema: worlds, stories, locations, entities, events, turns, npc_memory.
- Indices compostos por `story_id`.
- ActionResolver.
- TurnProcessor.
- AgencyRules.
- Fallback deterministico.
- Replay e save/load.

DoD: turno completo persistido, replay funcionando e estado restauravel.

## Fase 2 - NPCs autonomos

- Memoria curta em Room.
- Resumo longo via LLM offline.
- TTL e compactacao.
- Reacao contextual a eventos canonicos.

## Fase 3 - Geracao procedural

- ProceduralSeedBuilder deterministico.
- Locations, NPCs e eventos sob demanda.
- Reprodutibilidade por seed.

## Fase 4 - UX de jogo

- Tela principal de jogo.
- Historico de turnos.
- Inventario/status.
- Onboarding e tutorial.
- Benchmark de modelo.

## Fase 5 - Polimento e distribuicao

- Otimizacao de bateria e memoria.
- Configuracoes.
- Politica de distribuicao para conteudo adulto.
- Licenca e nome comercial.

## Riscos ativos

| Risco | Mitigacao |
| --- | --- |
| API llama.cpp muda | Pinar tag e documentar API usada |
| Android 4GB estoura RAM | Validar em device; fallback 6GB se necessario |
| Recarregar modelo em resume piora UX | Usar `onTrimMemory`, nao liberar sempre em `onPause` |
| Play Store rejeita +18 | Preparar sideload/F-Droid/AAB direto |
| GGUF grande | Resume + SHA256 + Wi-Fi warning |

## Proximos passos imediatos

1. Instalar APK mockado no device.
2. Validar navegacao e telas.
3. Adicionar testes de contrato JSON mais completos.
4. Integrar llama.cpp somente depois do teste em device com mock passar.

# PRD - Mundo Vivo 2.0 Mobile

## 📋 Problema Original

Análise e implementação de app mobile Android para o projeto **Mundo Vivo 2.0** (repositório privado: https://github.com/JuliMaiTan/mundovivo2.0).

**Requisitos Críticos**:
- **Plataforma**: Android nativo (Kotlin + Jetpack Compose)
- **LLM Local**: Rodando em celular 4GB RAM
- **Conteúdo**: Livre para +18 (violência explícita, sensualidade)
- **Modelos**: Uncensored/abliterated (Qwen 1.5B, Gemma 2B)
- **Offline-first**: Sem dependência de servidor

## 🎯 Arquitetura Aprovada

### Princípio Fundamental
> **O LLM nunca altera o mundo diretamente. Ele apenas narra fatos autorizados pelo motor.**

### Separação de Responsabilidades
```
Motor Canônico (Kotlin/Room)  = Verdade determinística
LLM Narrador (llama.cpp)       = Prosa autorizada com GBNF
```

## 👥 User Personas

### Persona 1: Jogador de RPG Solo
- 25-45 anos
- Celular 4-6GB RAM Android
- Quer narrativa procedural rica sem censura
- Valoriza offline (privacidade + custo)

### Persona 2: Escritor/Criador
- Usa como ferramenta de worldbuilding
- Aprecia consistência narrativa
- Quer conteúdo adulto sem filtros excessivos

## 🏗️ Requisitos Core

### Funcionais
- [x] Detecção automática de RAM
- [x] Recomendação inteligente de modelo (Qwen 4GB / Gemma 8GB)
- [x] Download de modelo com resume + SHA256
- [x] Validação de contrato JSON com GBNF
- [x] Detecção de violação de agência
- [x] Fallback determinístico
- [ ] Motor de jogo (Room DB + ActionResolver)
- [ ] Geração procedural de mundos
- [ ] NPCs autônomos com memória
- [ ] Sistema de ações confirmadas (chance/risco)

### Não-Funcionais
- [x] Suporte ARM64 e ARMv7
- [x] Kotlin 1.9+ e Jetpack Compose Material 3
- [x] Room + Coroutines + Flow
- [ ] llama.cpp integrado via JNI
- [ ] 4GB RAM viável (Qwen)
- [ ] 17-20 tok/s (baseline POC)

## 📊 Modelos LLM (Baseado em POC Real)

### Qwen 1.5B Q4_K_M (Padrão)
- Tamanho: 1.2GB
- Perf: 17-20 tok/s, 1.56 GB PSS
- PT-BR: aceitável (não excelente)
- **GBNF obrigatório** para JSON válido
- Recomendado: 4GB+ RAM

### Gemma 2B Q4_K_M (Qualidade)
- Tamanho: 1.7GB
- Perf: 7-8 tok/s, 2.7 GB PSS
- PT-BR: melhor que Qwen
- **GBNF obrigatório**
- Recomendado: 8GB+ RAM

## ✅ Implementado (Jan 2026)

### Fase 0 - Bootstrap (Concluído)

**Estrutura Android Base**
- Gradle Kotlin DSL (settings, build, properties)
- AndroidManifest + ProGuard
- Recursos (strings, colors, themes)
- Gradle wrapper

**Módulo util/**
- `DeviceInfo.kt`: Detecção RAM/storage/ABI/Android version
- `RamFormatter.kt`: Formatação MB/GB
- `Ids.kt`: Gerador UUID
- `Time.kt`: Utilitários

**Módulo llm/**
- `ModelTypes.kt`: Tipos base (ModelId, ModelInfo, DownloadProgress)
- `ModelCatalog.kt`: Qwen + Gemma com metadados
- `ModelSelectionPolicy.kt`: Recomendação por RAM (4/6/8GB)
- `ModelIntegrityChecker.kt`: SHA256 validation
- `ModelDownloader.kt`: OkHttp resumable
- `ModelManager.kt`: Coordenador central
- `LlamaEngine.kt`: JNI wrapper (com mock para Fase 0)
- `LlamaNativeBridge.kt`: Interface JNI
- `ChatMLFormatter.kt`: Templates ChatML/Gemma/Llama3/Alpaca
- `PromptBuilder.kt`: Constrói prompt com WorldRules
- `NarrativeGenerator.kt`: Orquestração + fallback
- `NarratorContract.kt`: Data classes + validador + detector agência
- `narrator_contract_v1.gbnf`: Grammar GBNF obrigatória

**Módulo ui/**
- `Theme.kt` + `Type.kt`: Material 3
- `ModelSelectionScreen.kt`: Seleção com detecção RAM
- `ModelDownloadScreen.kt`: Progress + checksum + retry
- `LLMTestScreen.kt`: Teste taverna com métricas
- `NavGraph.kt`: Navegação completa

**Native (Preparado)**
- `CMakeLists.txt`: Estrutura
- `llama-jni.cpp`: Stub preparado

**Documentação**
- README.md
- ARCHITECTURE.md
- STATUS.md
- LLAMA_INTEGRATION.md (guia completo)

## 🚧 Backlog Prioritizado

### P0 - Bloqueadores Fase 0
- [ ] Integrar llama.cpp como submodule
- [ ] Compilar biblioteca nativa (JNI)
- [ ] Substituir mock LlamaEngine por implementação real
- [ ] Testar em celular físico via ADB
- [ ] Validar métricas (17-20 tok/s Qwen)

### P1 - Fase 1 (Motor Core)
- [ ] Room database completo (todas as tabelas)
- [ ] `GameEngine` + `TurnProcessor`
- [ ] `ActionParser` (texto → ação estruturada)
- [ ] `ActionResolver` (aplica regras + gera fatos)
- [ ] `EventBus` (pub/sub eventos)
- [ ] `WorldStateRepository`
- [ ] `TurnRepository`
- [ ] Testes unitários motor

### P1 - Fase 2 (Criação Mundo)
- [ ] `WorldCreationFlow` (wizard 12 etapas)
- [ ] `WorldCreationDraft` + DataStore
- [ ] `ProceduralSeedBuilder`
- [ ] `WorldGenerator` + `LocationGenerator` + `NpcGenerator`
- [ ] `CharacterGenerator` (personagem jogador)
- [ ] `InitialSituationGenerator` (cena inicial)
- [ ] Integração com LLM para descrição

### P2 - Fase 3 (NPCs)
- [ ] `NpcAutonomyEngine` (comportamento autônomo)
- [ ] `NpcMemoryService` (memória com TTL)
- [ ] Sistema de relacionamentos (trust/fear/love)
- [ ] Reações contextuais

### P2 - Fase 4 (Ação Confirmada)
- [ ] `ActionPreview` (chance/risco)
- [ ] `ActionConfirmationDialog`
- [ ] Fluxo: input → preview → confirmação → resolução

### P2 - Fase 5 (Polimento)
- [ ] `WorldSnapshotRepository` (checkpoint)
- [ ] Save/load múltiplo
- [ ] Tela de configurações (threads, batch, temp)
- [ ] `ModelBenchmarkScreen`
- [ ] Tutorial/onboarding
- [ ] Otimizações performance

## 🎨 Contrato JSON (GBNF)

```json
{
  "contract_version": "1.0",
  "response_type": "TURN_NARRATION",
  "narrative": "string",
  "sensory_focus": ["array"],
  "npcs_mentioned": ["array"],
  "tone": "tensao",
  "warnings": [],
  "error": null
}
```

**Tons permitidos**: terror, humor, sensualidade, melancolia, acao, contemplativo, cotidiano, tensao, maravilhamento

## 🛡️ WorldRules (não ContentPolicy)

Não é censura moral, é consistência narrativa:
- **AgencyRules**: Jogador nunca age sozinho
- **WorldRules**: Consistência do mundo
- **NarrativeBoundaries**: Limites ficcionais

## 📅 Timeline

| Data | Marco |
|------|-------|
| Jan 23, 2026 | Análise inicial + PDF |
| Jan 23, 2026 | Correções POC aplicadas (GBNF obrigatório) |
| Jan 23, 2026 | Arquitetura aprovada + Fase 0 iniciada |
| Jan 23, 2026 | **Fase 0 estruturada (~80% concluído)** |
| Próximo | Integrar llama.cpp real |
| Próximo | Testar em celular físico |

## 🎯 Next Actions (Ordem)

1. **Integrar llama.cpp** (guia em `/app/LLAMA_INTEGRATION.md`)
2. **Compilar biblioteca nativa** via NDK
3. **Testar em celular** com Qwen 1.5B
4. **Validar métricas** (tok/s, TTFT, RAM)
5. **Iniciar Fase 1** (Room DB + motor)

## 📊 Progresso Global

```
Fase 0 - Bootstrap:    ████████████████████ 80% (falta integração real llama.cpp)
Fase 1 - Motor:        ░░░░░░░░░░░░░░░░░░░░  0%
Fase 2 - Mundo:        ░░░░░░░░░░░░░░░░░░░░  0%
Fase 3 - NPCs:         ░░░░░░░░░░░░░░░░░░░░  0%
Fase 4 - Ações:        ░░░░░░░░░░░░░░░░░░░░  0%
Fase 5 - Polimento:    ░░░░░░░░░░░░░░░░░░░░  0%
```

**Total**: ~15% do projeto completo. Fase 0 estabelece toda a fundação técnica.

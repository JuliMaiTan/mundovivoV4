# Mundo Vivo 2.0 - Mobile RPG com LLM Local

## 🎮 Visão Geral

Mundo Vivo é um RPG procedural para Android com narrador LLM rodando localmente no dispositivo. O jogo separa claramente o **motor canônico** (verdade do mundo) do **LLM narrador** (prosa autorizada), garantindo consisteência e agência do jogador.

## 🛠️ Arquitetura

### Separação Motor vs LLM

```
Input do jogador
    ↓
Motor Canônico (determina verdade)
    ↓
Fatos Autorizados
    ↓
LLM Narrador (gera prosa com GBNF)
    ↓
Narrativa Validada
    ↓
UI
```

**Regra de Ouro**: Se afeta o jogo, pertence ao motor. Se só descreve, pode ir para o LLM.

### Stack Técnico

- **Linguagem**: Kotlin 1.9+
- **UI**: Jetpack Compose + Material 3
- **Persistência**: Room (SQLite)
- **LLM**: llama.cpp Android (via JNI)
- **Modelos**: Qwen 1.5B / Gemma 2B (Q4_K_M, abliterated)
- **Async**: Coroutines + Flow
- **Build**: Gradle 8+ com Kotlin DSL

## 📱 Modelos Suportados

### Qwen 1.5B Q4 (Padrão)
- ⚡ 17-20 tok/s em 4GB RAM
- 💾 1.2GB download, 1.56GB RAM
- 🌍 PT-BR aceitável
- ✅ Viável em 4GB+

### Gemma 2B Q4 (Qualidade)
- 📖 7-8 tok/s
- 💾 1.7GB download, 2.7GB RAM
- 🌍 PT-BR melhor que Qwen
- ✅ Recomendado 8GB+

## 🚧 Fases de Desenvolvimento

### Fase 0 - Bootstrap LLM ⚙️ (Scaffold pronto, build/device pendente)
- [x] Estrutura Android base
- [x] DeviceInfo (detecção RAM)
- [x] ModelCatalog (Qwen/Gemma)
- [x] ModelSelectionPolicy
- [x] ModelDownloader (resumable, corrigido)
- [x] ModelIntegrityChecker (SHA256)
- [x] ModelManager
- [x] GBNF grammar
- [x] NarratorContractValidator
- [x] UI de seleção de modelo
- [x] Tela de teste taverna + métricas (LLMTestScreen)
- [x] Navegação (NavGraph)
- [x] LlamaEngine (mock ativo — permite UI funcionar sem native lib)
- [ ] Integração llama.cpp + JNI (guia em LLAMA_INTEGRATION.md)
- [ ] Build validado via `./gradlew assembleDebug`
- [ ] SHA256 real dos modelos (placeholders ativos)
- [ ] Rodar em celular físico

**Aceite Fase 0**: Download Qwen, gera narrativa válida com GBNF, métricas (tok/s, TTFT, RAM) exibidas.

### Fase 1 - Motor Core
- [ ] Room database (todas as tabelas)
- [ ] GameEngine + TurnProcessor
- [ ] ActionParser + ActionResolver
- [ ] EventBus
- [ ] WorldStateRepository
- [ ] Testes unitários

### Fase 2 - Criação de Mundo
- [ ] WorldCreationFlow (wizard 12 etapas)
- [ ] Gerador procedural (mundo, NPCs, locações)
- [ ] Integração LLM (cena inicial)

### Fase 3 - NPCs e Simulação
- [ ] NpcAutonomyEngine
- [ ] Memória de NPCs
- [ ] Sistema de relacionamentos

### Fase 4 - Ação Confirmada
- [ ] ActionPreview (chance/risco)
- [ ] ActionConfirmationDialog

### Fase 5 - Polimento
- [ ] Save/load múltiplo
- [ ] Configurações
- [ ] Tutorial
- [ ] Otimizações

## 📝 Contrato JSON (GBNF Obrigatório)

```json
{
  "contract_version": "1.0",
  "response_type": "TURN_NARRATION",
  "narrative": "Texto narrativo em português BR.",
  "sensory_focus": ["visao", "audicao"],
  "npcs_mentioned": ["Torvin"],
  "tone": "tensao",
  "warnings": [],
  "error": null
}
```

**Importante**: GBNF é obrigatório. Modelos 1.5B não seguem schema consistentemente sem constrained decoding.

## 🛡️ WorldRules (não ContentPolicy)

O jogo permite conteúdo adulto (+18), mas com foco em:
- **AgencyRules**: Jogador nunca age sozinho
- **WorldRules**: Consistência do mundo
- **NarrativeBoundaries**: Limites ficcionais

Não é censura moral, é consistência narrativa.

## 👥 Time

Projeto desenvolvido para celulares Android 4GB+ RAM com LLM local uncensored.

## 📝 Licença

TODO: Definir licença

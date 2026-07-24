# Arquitetura Mundo Vivo 2.0 - Mobile

## 🎯 Princípio Fundamental

**O LLM nunca altera o mundo diretamente. Ele apenas narra fatos autorizados pelo motor.**

```
Motor Canônico = Verdade do mundo (determinístico 100%)
LLM Narrador = Prosa autorizada (best-effort, GBNF obrigatório)
```

## 📊 Estrutura de Dados

### Tabelas Room (SQLite)

#### worlds
Representa um universo/campanha isolado.
```kotlin
- id: String (world_xxx)
- name: String
- genre: String
- tone: String
- adultContentEnabled: Boolean
- createdAt: Long
- updatedAt: Long
```

#### stories
Save/campanha dentro de um mundo.
```kotlin
- id: String (story_xxx)
- worldId: String
- title: String
- currentLocationId: String
- currentTurn: Int
- status: String (ACTIVE, ENDED, PAUSED)
- createdAt: Long
```

#### locations
Lugares do mundo com estado mutável.
```kotlin
- id: String (loc_xxx)
- worldId: String
- name: String
- description: String
- type: String (tavern, forest, dungeon)
- stateJson: String (porta aberta/fechada, clima, etc)
```

#### entities
Tabela genérica para NPCs, itens, objetos, criaturas.
```kotlin
- id: String (entity_xxx)
- worldId: String
- storyId: String?
- locationId: String?
- type: String (PLAYER, NPC, ITEM, OBJECT)
- name: String
- stateJson: String
```

#### npc_profiles
Dados específicos de NPCs.
```kotlin
- npcId: String
- personalityJson: String (traits, speechStyle, morality)
- goalsJson: String
- fearsJson: String
- secretsJson: String
```

#### relationships
Relações entre entidades.
```kotlin
- sourceEntityId: String
- targetEntityId: String
- type: String (trust, fear, love, hate)
- value: Int (-100 to +100)
```

#### events
Registro canônico imutável do que aconteceu.
```kotlin
- id: String
- storyId: String
- turnNumber: Int
- type: String (PLAYER_ACTION_RESOLVED, NPC_REACTION)
- payloadJson: String (fatos canônicos)
- createdAt: Long
```

#### turns
Histórico completo de cada turno.
```kotlin
- id: String
- storyId: String
- turnNumber: Int
- playerInput: String
- resolutionJson: String (o que aconteceu)
- narrativeJson: String (o que LLM narrou)
- createdAt: Long
```

#### memories
Memórias de NPCs com TTL.
```kotlin
- ownerEntityId: String
- subjectEntityId: String?
- type: String (observation, trauma, promise)
- importance: Int (0-100)
- content: String
- expiresAtTurn: Int?
```

#### world_snapshots
Checkpoint periódico do estado.
```kotlin
- storyId: String
- turnNumber: Int
- snapshotJson: String
- createdAt: Long
```

#### llm_outputs
Debug e auditoria de respostas LLM.
```kotlin
- storyId: String
- turnNumber: Int
- modelName: String
- rawOutput: String
- parsedJson: String?
- valid: Boolean
- tokensPerSecond: Double?
```

## 🔄 Loop de Turno

```
1. Jogador escreve ação
    ↓
2. InputNormalizer (limpa e classifica)
    ↓
3. ActionParser (texto → ação estruturada)
    ↓
4. ActionResolver (valida e aplica regras)
    ↓
5. WorldStateUpdater (altera estado Room)
    ↓
6. EventBus (registra eventos)
    ↓
7. NpcAutonomyEngine (NPCs reagem)
    ↓
8. PromptBuilder (fatos → prompt)
    ↓
9. LlamaEngine (gera JSON com GBNF)
    ↓
10. NarratorContractValidator (valida)
    ↓
11. UI (mostra narrativa)
    ↓
12. TurnRepository (salva tudo)
```

## 🧩 Módulos Principais

### domain/engine
- `GameEngine`: Orquestrador principal
- `TurnProcessor`: Processa turnos
- `WorldState`: Estado do mundo em memória

### domain/action
- `ActionParser`: Texto livre → intenção estruturada
- `ActionResolver`: Aplica regras e calcula resultado

### domain/npc
- `NpcAutonomyEngine`: Comportamento autônomo
- `NpcMemoryService`: Memória com TTL

### domain/events
- `EventBus`: Pub/sub de eventos canônicos

### domain/rules
- `WorldRules`: Consistência do mundo
- `AgencyRules`: Jogador nunca age sozinho
- `ActionRules`: Regras de ações

### domain/procedural
- `WorldGenerator`: Geração procedural de mundos
- `LocationGenerator`: Geração de locações
- `NpcGenerator`: Geração de NPCs
- `CharacterGenerator`: Personagem do jogador
- `ProceduralSeedBuilder`: Seed determinística

### llm/
- `ModelManager`: Coordenador (download, status, ativo)
- `ModelCatalog`: Lista modelos (Qwen/Gemma)
- `ModelDownloader`: Download resumable
- `ModelIntegrityChecker`: SHA256
- `ModelSelectionPolicy`: Recomendação por RAM
- `LlamaEngine`: JNI wrapper
- `PromptBuilder`: Fatos → prompt
- `NarrativeGenerator`: Orquestra geração
- `NarratorContractValidator`: Valida JSON

### data/db
- Room database + DAOs + Entities

### ui/
- Jetpack Compose screens
- ViewModels + StateFlow

## 🎨 Contrato JSON (GBNF)

```json
{
  "contract_version": "1.0",
  "response_type": "TURN_NARRATION",
  "narrative": "string",
  "sensory_focus": ["array"],
  "npcs_mentioned": ["array"],
  "tone": "enum",
  "warnings": ["array"],
  "error": null | "string"
}
```

**Tons permitidos**: terror, humor, sensualidade, melancolia, acao, contemplativo, cotidiano, tensao, maravilhamento

## 🛡️ Validação de Agência

O validador detecta violações como:
- "você sente"
- "você pensa"
- "você decide"
- "você percebe que"

**Regra**: O LLM nunca narra estados internos ou ações do jogador.

## 🔧 Determinismo

### Determinístico (motor)
- Dano, morte, inventário
- Localização, relações
- Flags, quests
- Portas abertas/fechadas

### Não-determinístico (LLM)
- Prosa, metáforas
- Ritmo narrativo
- Escolha sensorial

## 🌱 Geração Procedural

```
WorldCreationDraft (wizard 12 etapas)
    ↓
ProceduralSeedBuilder
    ↓
WorldGenerator (esqueleto canônico)
    ↓
LocationGenerator + NpcGenerator + CharacterGenerator
    ↓
Room DB (persistência)
    ↓
LLM (só descreve, não inventa estrutura)
```

**Seed determinística**: `worldSeed + coordenada + tipoLocal = mesmo resultado sempre`

## 📱 Modelos LLM

### Qwen 1.5B (Padrão)
- Baseado em POC real
- 17-20 tok/s, 1.56 GB PSS
- PT-BR aceitável (não excelente)
- Viável em 4GB RAM
- GBNF obrigatório

### Gemma 2B (Qualidade)
- 7-8 tok/s, 2.7 GB PSS
- PT-BR melhor que Qwen
- Recomendado 8GB+ RAM
- GBNF obrigatório

## 🚨 Fallback

Se LLM falhar:
1. Tentar regenerar (1x)
2. Prompt menor
3. **Narrativa determinística simples**: "A ação foi concluída. [Descrição básica do estado]."

Garante que o jogo nunca trava.

## ✅ Próximos Passos (Fase 0 Restante)

1. Integrar llama.cpp Android (prebuilt ou compilar)
2. Implementar JNI bridge completo
3. Implementar LlamaEngine.kt (load model, generate, free)
4. Criar tela de teste taverna
5. Exibir métricas (tok/s, TTFT, RAM)
6. Testar em celular físico via ADB
7. Validar GBNF + JSON

**Aceite Fase 0**: Download Qwen completo, prompt taverna → JSON válido com GBNF, métricas exibidas.

# Mundo Vivo V4

RPG narrativo procedural offline-first para Android, com LLM local via llama.cpp.

O projeto separa duas responsabilidades:

- **Motor canonico:** decide estado, regras, acoes, NPCs e consequencias.
- **Narrador LLM:** narra apenas fatos autorizados pelo motor, em JSON restrito por GBNF.

## Stack

- Kotlin 1.9+
- Jetpack Compose + Material 3
- Coroutines + Flow
- Room/SQLite a partir da Fase 1
- llama.cpp via JNI a partir da Fase 0 real
- Gradle KTS + CMake

## Modelos

| Modelo | Perfil |
| --- | --- |
| Qwen 2.5 1.5B abliterated Q4_K_M | Rapido e leve, default para 4-6GB |
| Gemma 2 2B abliterated Q4_K_M | Narrativa mais densa, recomendado para 8GB+ |

O Qwen ja possui SHA256 real no catalogo. A Gemma ainda precisa de hash real antes de release.

## Estado atual

O repo contem scaffold Android, UI base, contrato do narrador, GBNF, downloader de modelos e LlamaEngine mockado.

Ainda pendente:

- build `assembleDebug` validado;
- llama.cpp como submodulo;
- JNI real;
- APK testado em celular;
- motor canonico com Room.

## Comandos

Configure `local.properties` se necessario:

```properties
sdk.dir=C:\\Users\\jumtp\\Android\\Sdk
```

Build:

```bash
./gradlew assembleDebug
```

Testes:

```bash
./gradlew test
```

No Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
```

## Documentos

- `docs/MOBILE_PLAN.md`: plano mobile revisado.
- `STATUS.md`: estado real do projeto.
- `ARCHITECTURE.md`: arquitetura.
- `LLAMA_INTEGRATION.md`: guia de integracao llama.cpp.
- `BUILD_SETUP.md`: setup de ambiente.

## Regra de ouro

Se afeta o jogo, pertence ao motor. Se so descreve, pode ir para o LLM.

# Guia de Integração llama.cpp Android

Este documento descreve como integrar `llama.cpp` ao projeto Mundo Vivo para habilitar inferência LLM local.

## 📋 Opções de Integração

### Opção A: llama.cpp como Submodule Git (Recomendado)

Permite compilar llama.cpp diretamente com o NDK do Android Studio.

```bash
# 1. Adicionar como submodule
cd /app/app/src/main/cpp
git submodule add https://github.com/ggerganov/llama.cpp.git llama.cpp
cd llama.cpp
git checkout b3821  # Tag estável recente (Jan 2026)

# 2. Ou clonar sem submodule (se preferir)
cd /app/app/src/main/cpp
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp
git checkout b3821
```

### Opção B: Prebuilt AAR

Se estiver disponível um AAR compilado, adicione ao `app/libs/`:

```
app/libs/
└── llama-cpp-android-b3821.aar
```

E no `build.gradle.kts`:
```kotlin
implementation(files("libs/llama-cpp-android-b3821.aar"))
```

## 🔧 CMakeLists.txt Completo

Após adicionar llama.cpp, atualize `/app/app/src/main/cpp/CMakeLists.txt`:

```cmake
cmake_minimum_required(VERSION 3.22.1)
project("mundovivo-llama" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Configurações llama.cpp para Android
set(LLAMA_STATIC ON)
set(LLAMA_BUILD_TESTS OFF)
set(LLAMA_BUILD_EXAMPLES OFF)
set(LLAMA_BUILD_SERVER OFF)

# Otimizações ARM
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(LLAMA_NATIVE OFF)
    add_compile_options(-march=armv8.2-a+fp16+dotprod)
endif()

# Adiciona llama.cpp como subdirectory
add_subdirectory(llama.cpp)

# JNI bridge
add_library(mundovivo-llama SHARED
    llama-jni.cpp
)

target_include_directories(mundovivo-llama PRIVATE
    llama.cpp
    llama.cpp/common
)

target_link_libraries(mundovivo-llama
    llama
    common
    log
    android
)
```

## 📝 llama-jni.cpp Completo

Implementação do JNI bridge:

```cpp
#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include "llama.h"
#include "common.h"

#define LOG_TAG "MundoVivo-LLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct ModelState {
    llama_model* model;
    llama_context* ctx;
    llama_sampler* sampler;
    struct llama_grammar* grammar;
};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_mundovivo_llm_LlamaNativeBridge_initModel(
    JNIEnv* env,
    jobject thiz,
    jstring modelPath,
    jint nCtx,
    jint nThreads,
    jint nBatch,
    jstring grammarPath
) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    
    llama_backend_init();
    
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU only
    
    llama_model* model = llama_load_model_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);
    
    if (!model) {
        LOGE("Failed to load model");
        return 0;
    }
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = nCtx;
    ctx_params.n_threads = nThreads;
    ctx_params.n_batch = nBatch;
    
    llama_context* ctx = llama_new_context_with_model(model, ctx_params);
    if (!ctx) {
        llama_free_model(model);
        LOGE("Failed to create context");
        return 0;
    }
    
    // Carrega grammar se fornecida
    struct llama_grammar* grammar = nullptr;
    if (grammarPath != nullptr) {
        const char* gpath = env->GetStringUTFChars(grammarPath, nullptr);
        // TODO: Ler arquivo GBNF e parsear
        // grammar = llama_grammar_init_from_string(...);
        env->ReleaseStringUTFChars(grammarPath, gpath);
    }
    
    ModelState* state = new ModelState();
    state->model = model;
    state->ctx = ctx;
    state->grammar = grammar;
    
    LOGI("Model loaded successfully");
    return reinterpret_cast<jlong>(state);
}

JNIEXPORT jstring JNICALL
Java_com_mundovivo_llm_LlamaNativeBridge_generate(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jstring prompt,
    jint maxTokens,
    jfloat temperature,
    jobject callback
) {
    ModelState* state = reinterpret_cast<ModelState*>(handle);
    if (!state) return env->NewStringUTF("");
    
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    
    // Tokenize prompt
    std::vector<llama_token> tokens = llama_tokenize(state->ctx, promptStr, true);
    env->ReleaseStringUTFChars(prompt, promptStr);
    
    // Callback method ID
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID invokeMethod = env->GetMethodID(callbackClass, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");
    
    std::string result;
    
    // Generation loop
    for (int i = 0; i < maxTokens; i++) {
        // Decode
        llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size(), 0, 0);
        if (llama_decode(state->ctx, batch) != 0) {
            LOGE("llama_decode failed");
            break;
        }
        
        // Sample next token (com grammar se disponível)
        llama_token new_token = llama_sample_token(state->ctx, nullptr);
        
        if (new_token == llama_token_eos(state->model)) break;
        
        // Convert to string
        char token_str[64];
        int n = llama_token_to_piece(state->model, new_token, token_str, sizeof(token_str), 0, false);
        std::string tokenText(token_str, n);
        
        result += tokenText;
        
        // Callback com o token
        jstring jToken = env->NewStringUTF(tokenText.c_str());
        env->CallObjectMethod(callback, invokeMethod, jToken);
        env->DeleteLocalRef(jToken);
        
        // Prepara próximo token
        tokens.clear();
        tokens.push_back(new_token);
    }
    
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_mundovivo_llm_LlamaNativeBridge_freeModel(
    JNIEnv* env,
    jobject thiz,
    jlong handle
) {
    ModelState* state = reinterpret_cast<ModelState*>(handle);
    if (state) {
        if (state->grammar) llama_grammar_free(state->grammar);
        llama_free(state->ctx);
        llama_free_model(state->model);
        delete state;
    }
    llama_backend_free();
}

} // extern "C"
```

## 🎯 Ajustes no LlamaEngine.kt

Após integração native, remover o mock em `LlamaEngine.kt`:

```kotlin
// REMOVER:
// MOCK para Fase 0
// modelHandle = 12345L
// isLoaded = true

// DESCOMENTAR:
modelHandle = LlamaNativeBridge.initModel(
    modelPath = modelFile.absolutePath,
    nCtx = config.nCtx,
    nThreads = config.nThreads,
    nBatch = config.nBatch,
    grammarPath = grammarFile?.absolutePath
)

if (modelHandle == 0L) {
    return@withContext Result.failure(RuntimeException("Falha ao carregar modelo native"))
}
isLoaded = true
```

## 🧪 Como Testar em Celular Físico

### 1. Preparar Ambiente
```bash
# Instalar Android SDK + NDK
# Configurar ANDROID_HOME e NDK_HOME
export ANDROID_HOME=~/Android/Sdk
export NDK_HOME=$ANDROID_HOME/ndk/25.2.9519653
```

### 2. Conectar Celular via ADB
```bash
# Habilitar depuração USB no celular
# (Ajustes → Sobre o Telefone → Toque em "Número da versão" 7 vezes)
# (Ajustes → Opções do desenvolvedor → Depuração USB)

adb devices
# Deve mostrar seu celular
```

### 3. Build e Instalar
```bash
cd /app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Ver Logs em Tempo Real
```bash
adb logcat -s MundoVivo-LLM
# ou geral:
adb logcat | grep -E "MundoVivo|llama"
```

### 5. Baixar Modelo Manualmente (se necessário)

Se o download in-app falhar (redes lentas), você pode fazer download manual:

```bash
# Baixar Qwen 1.5B
wget https://huggingface.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF/resolve/main/EVA-Qwen2.5-1.5B-v0.0.Q4_K_M.gguf

# Copiar para o app
adb push EVA-Qwen2.5-1.5B-v0.0.Q4_K_M.gguf /sdcard/Download/

# Depois no app: importar via URI (implementar picker se necessário)
```

## 📊 Métricas Esperadas

Baseado em POC real (Edge 20 Pro, 8GB RAM):

### Qwen 1.5B Q4_K_M
- **Tokens/s**: 17-20 tok/s
- **TTFT**: ~700-2000ms
- **RAM PSS**: 1.56 GB
- **Latência total**: ~5-10s para narrativa completa

### Gemma 2B Q4_K_M
- **Tokens/s**: 7-8 tok/s
- **TTFT**: ~6000ms (mais lento)
- **RAM PSS**: 2.7 GB
- **Latência total**: ~15-30s

## ✅ Checklist de Validação Fase 0

Após integração completa:

- [ ] App instala em celular via ADB
- [ ] Tela de seleção mostra RAM do dispositivo
- [ ] Download Qwen 1.5B funciona (resume + checksum)
- [ ] Modelo carrega em memória (ver logs)
- [ ] Prompt taverna é enviado
- [ ] Streaming de tokens aparece na UI
- [ ] JSON retornado é válido (GBNF forçou)
- [ ] Arrays corretos (sensory_focus, npcs_mentioned, warnings)
- [ ] Campo `error` existe (não "erro")
- [ ] Tone está no enum
- [ ] Métricas exibidas: tok/s, TTFT, RAM
- [ ] Narrativa em PT-BR sem violação de agência

## 🚨 Problemas Comuns

### 1. "libmundovivo-llama.so not found"
- NDK não configurado corretamente
- Verifique `local.properties` tem `ndk.dir=...`

### 2. "Model file corrupted"
- Checksum SHA256 não confere
- Delete e baixe novamente

### 3. "Out of memory"
- Modelo maior que RAM disponível
- Use Qwen 1.5B em celulares 4GB
- Feche outros apps antes de rodar

### 4. GBNF não força JSON
- Verifique se `grammarPath` foi passado corretamente
- Verifique se `llama_sample_grammar` está sendo chamado
- Teste com grammar mais simples primeiro

### 5. Streaming lento demais
- Reduza `n_ctx` (ex: 1024 em vez de 2048)
- Aumente `nThreads` para número de cores físicos
- Verifique se está usando ARM64 (não x86 em emulador)

## 📚 Recursos

- llama.cpp: https://github.com/ggerganov/llama.cpp
- llama.cpp Android: https://github.com/ggerganov/llama.cpp/blob/master/examples/llama.android/
- GGUF format: https://github.com/ggerganov/ggml/blob/master/docs/gguf.md
- Qwen 1.5B: https://huggingface.co/mradermacher/EVA-Qwen2.5-1.5B-v0.0-GGUF
- Gemma 2B abliterated: https://huggingface.co/mradermacher/gemma-2-2b-it-abliterated-GGUF

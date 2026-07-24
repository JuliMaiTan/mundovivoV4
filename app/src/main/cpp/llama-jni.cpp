// llama-jni.cpp
// JNI bridge para llama.cpp
// TODO Fase 0: Implementação completa após integração llama.cpp

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "MundoVivo-LLM"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// TODO: Implementar métodos JNI
// JNIEXPORT jlong JNICALL Java_com_mundovivo_llm_LlamaNativeBridge_initModel(...)
// JNIEXPORT jstring JNICALL Java_com_mundovivo_llm_LlamaNativeBridge_generate(...)
// JNIEXPORT void JNICALL Java_com_mundovivo_llm_LlamaNativeBridge_freeModel(...)

} // extern "C"

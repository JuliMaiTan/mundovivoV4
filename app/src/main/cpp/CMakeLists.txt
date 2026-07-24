# CMakeLists.txt para llama.cpp Android
# Fase 0: Estrutura preparada, implementação completa na próxima etapa

cmake_minimum_required(VERSION 3.22.1)

project("mundovivo-llama" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# TODO Fase 0: Adicionar llama.cpp como submódulo ou prebuilt
# add_subdirectory(llama.cpp)

# TODO Fase 0: JNI bridge
# add_library(mundovivo-llama SHARED
#     llama-jni.cpp
# )

# target_link_libraries(mundovivo-llama
#     llama
#     log
# )

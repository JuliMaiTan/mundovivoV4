# Setup Local — Do zero até `./gradlew assembleDebug`

Você já está no terminal remoto. Este guia é a sequência exata para desbloquear o build.

## Onde você está agora

- ✅ Terminal remoto (VS Code) acessível
- ✅ Java 17 (ARM64) instalado: `/usr/lib/jvm/java-17-openjdk-arm64`
- ✅ Gradle 8.2 funcional (`/tmp/gradle-8.2/bin/gradle`)
- ❌ Android SDK ausente ← **próximo gate**
- ❌ Gradle wrapper (`./gradlew`) ainda não gerado ← depende do SDK

## Passo 1 — Instalar Android SDK

Rode o script que já preparei:

```bash
sudo bash /app/scripts/setup-android-sdk.sh
```

O que ele faz:
1. Baixa `commandlinetools-linux-11076708_latest.zip` do Google
2. Instala em `/opt/android-sdk/cmdline-tools/latest/`
3. Aceita licenças automaticamente (`sdkmanager --licenses`)
4. Instala `platform-tools`, `platforms;android-34`, `build-tools;34.0.0`
5. Escreve `/app/local.properties` com `sdk.dir=/opt/android-sdk`

Tamanho aproximado: ~500 MB de download.

**Se preferir manual**, o mínimo é:

```bash
mkdir -p /opt/android-sdk/cmdline-tools
cd /tmp
curl -L -o cmdtools.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q cmdtools.zip
mv cmdline-tools /opt/android-sdk/cmdline-tools/latest

export ANDROID_HOME=/opt/android-sdk
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# local.properties
echo "sdk.dir=/opt/android-sdk" > /app/local.properties
```

## Passo 2 — Exportar env vars na sessão

Coloque isso no `~/.bashrc` (ou rode por sessão):

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Depois:
```bash
source ~/.bashrc   # (se editou o bashrc)
```

## Passo 3 — Gerar o Gradle wrapper

```bash
cd /app
/tmp/gradle-8.2/bin/gradle wrapper --gradle-version 8.2
```

Isso cria:
- `/app/gradlew`
- `/app/gradlew.bat`
- `/app/gradle/wrapper/gradle-wrapper.jar`

O `.jar` **fica fora do Git** (já está no `.gitignore`), mas é regenerável a qualquer momento.

## Passo 4 — Primeiro build

```bash
cd /app
./gradlew assembleDebug --info
```

Espere alguns minutos na primeira vez (Gradle baixa dependências: AndroidX, Compose, Room, OkHttp, kotlinx-serialization, kotlinx-coroutines).

Se tudo der certo, você terá:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Passo 5 — Instalar no celular

```bash
# Confirme que o celular está visível
adb devices

# Instale
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Logs em tempo real
adb logcat -s MundoVivo-LLM ModelManager
```

## Erros prováveis e como resolver

### `Failed to install the following SDK components: [...] Accept licenses first`
```bash
yes | sdkmanager --licenses
```

### `SDK location not found. Define ANDROID_HOME`
Certifique-se que `/app/local.properties` existe e contém `sdk.dir=/opt/android-sdk`.
Alternativa: `export ANDROID_HOME=/opt/android-sdk` antes de rodar Gradle.

### `Unsupported Java. Your build is currently configured to use JDK 21`
O AGP 8.2 exige JDK 17. Confirme:
```bash
java -version   # deve mostrar 17.x
echo $JAVA_HOME # deve apontar para openjdk-17
```

### `Namespace not specified`
Não deve acontecer — `namespace = "com.mundovivo"` já está em `app/build.gradle.kts`.

### `Cannot find the manifest file: AndroidManifest.xml`
Já existe em `/app/app/src/main/AndroidManifest.xml`. Se o build reclamar, confirme que você está em `/app` ao rodar `./gradlew`.

### Build morre com `kotlin.plugin.serialization` unresolved
Já corrigido — o plugin está em `build.gradle.kts` (root e app). Se aparecer, delete `.gradle/` e refaça.

## Depois que `assembleDebug` passar

Você não vai precisar do NDK ainda porque o `LlamaEngine.kt` está MOCKED (retorna JSON fake). O APK vai:
- Detectar RAM do celular
- Simular download de modelo
- Rodar o "teste taverna" com resposta mockada
- Mostrar todas as telas (Selection → Download → Test)

Isso é suficiente para validar UI + navegação + Compose + Room (quando entrar).

## Próximo gate (depois do assembleDebug OK)

Integrar llama.cpp REAL:

```bash
# Instalar NDK e CMake
sdkmanager "ndk;25.2.9519653" "cmake;3.22.1"

# Clonar llama.cpp
cd /app/app/src/main/cpp
git clone --depth 1 --branch b3821 https://github.com/ggerganov/llama.cpp

# Seguir /app/LLAMA_INTEGRATION.md para completar CMakeLists.txt e llama-jni.cpp
```

## Resumo do estado atual

| Item | Status |
|------|--------|
| Java 17 | ✅ instalado |
| Gradle 8.2 | ✅ funcional |
| Android SDK | ⏳ próximo passo (script pronto) |
| Gradle wrapper | ⏳ depende do SDK |
| assembleDebug | ⏳ meta imediata |
| llama.cpp real | ⏳ depois do build passar |
| Rodar em celular | ⏳ depois do APK gerado |

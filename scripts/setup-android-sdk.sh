#!/usr/bin/env bash
# ============================================================================
# setup-android-sdk.sh
# Instala Android SDK command-line tools + platforms/build-tools mínimos para
# rodar `./gradlew assembleDebug` no Mundo Vivo 2.0.
#
# Uso:
#   sudo bash /app/scripts/setup-android-sdk.sh
#   OU sem sudo se você tiver permissão em /opt
#
# Requisitos já resolvidos:
#   - JDK 17 (ARM64) instalado em /usr/lib/jvm/java-17-openjdk-arm64
#   - Gradle 8.2 disponível (via /tmp/gradle-8.2 ou local)
# ============================================================================
set -euo pipefail

# --- Configuração -----------------------------------------------------------
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/root/android-sdk}"
CMDLINE_TOOLS_VERSION="11076708"   # commandlinetools-linux-*_latest.zip (Aug 2024)
COMPILE_SDK="34"
BUILD_TOOLS="34.0.0"

# Detecta arquitetura (o ZIP do commandline-tools é multi-arch, mas
# o JDK e algumas ferramentas mudam por ABI)
UNAME_M=$(uname -m)
echo "[info] Host arch: $UNAME_M"
echo "[info] SDK destino: $ANDROID_SDK_ROOT"

# --- Pré-requisitos ---------------------------------------------------------
command -v unzip >/dev/null || { echo "[erro] unzip não instalado. Rode: apt install -y unzip"; exit 1; }
command -v curl  >/dev/null || { echo "[erro] curl  não instalado. Rode: apt install -y curl";  exit 1; }
command -v java  >/dev/null || { echo "[erro] JDK 17 não encontrado. Rode: apt install -y openjdk-17-jdk"; exit 1; }

JAVA_VERSION=$(java -version 2>&1 | head -n1)
echo "[info] $JAVA_VERSION"

# --- Download commandline-tools --------------------------------------------
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
cd /tmp

CMDLINE_ZIP="commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
if [ ! -f "$CMDLINE_ZIP" ]; then
    echo "[step] Baixando $CMDLINE_ZIP ..."
    curl -L -o "$CMDLINE_ZIP" \
        "https://dl.google.com/android/repository/${CMDLINE_ZIP}"
fi

# --- Instalação -------------------------------------------------------------
echo "[step] Extraindo commandline-tools..."
rm -rf /tmp/cmdline-tools-extract
mkdir -p /tmp/cmdline-tools-extract
unzip -q -o "$CMDLINE_ZIP" -d /tmp/cmdline-tools-extract

# Google exige que o executável fique em cmdline-tools/latest/bin/
rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
mv /tmp/cmdline-tools-extract/cmdline-tools "$ANDROID_SDK_ROOT/cmdline-tools/latest"

# --- PATH temporário --------------------------------------------------------
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

echo "[info] sdkmanager: $(which sdkmanager)"

# --- Aceitar licenças (idempotente) ----------------------------------------
echo "[step] Aceitando licenças..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true

# --- Instalar componentes mínimos ------------------------------------------
echo "[step] Instalando platforms;android-${COMPILE_SDK} + build-tools;${BUILD_TOOLS} + platform-tools..."
sdkmanager \
    "platform-tools" \
    "platforms;android-${COMPILE_SDK}" \
    "build-tools;${BUILD_TOOLS}"

# --- Escrever /app/local.properties ----------------------------------------
LOCAL_PROPS="/app/local.properties"
echo "[step] Escrevendo $LOCAL_PROPS"
cat > "$LOCAL_PROPS" <<EOF
# Gerado por setup-android-sdk.sh — NÃO commitar (já está no .gitignore)
sdk.dir=$ANDROID_SDK_ROOT
EOF

# --- Resumo -----------------------------------------------------------------
echo ""
echo "========================================================================"
echo " Android SDK pronto."
echo "========================================================================"
echo " ANDROID_SDK_ROOT = $ANDROID_SDK_ROOT"
echo " compileSdk       = $COMPILE_SDK"
echo " buildTools       = $BUILD_TOOLS"
echo ""
echo " Adicione ao seu shell (~/.bashrc ou por sessão):"
echo ""
echo "   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64"
echo "   export ANDROID_HOME=$ANDROID_SDK_ROOT"
echo "   export ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
echo '   export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"'
echo ""
echo " Depois:"
echo "   cd /app"
echo "   /tmp/gradle-8.2/bin/gradle wrapper --gradle-version 8.2"
echo "   ./gradlew assembleDebug"
echo ""
echo " NDK (só necessário quando integrar llama.cpp real, não agora):"
echo "   sdkmanager 'ndk;25.2.9519653' 'cmake;3.22.1'"
echo "========================================================================"

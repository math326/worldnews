#!/bin/bash
# Script de instalação do Android SDK e build do WorldNews APK
# Execute: bash INSTALL.sh

set -e

ANDROID_SDK_DIR="$HOME/Android/Sdk"
CMD_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

echo "=== WorldNews — Setup Android SDK ==="

# 1. Verificar Java
if ! command -v java &>/dev/null; then
    echo "ERRO: Java não encontrado. Instale o JDK 17 ou superior."
    exit 1
fi
echo "✓ Java: $(java -version 2>&1 | head -1)"

# 2. Instalar Android SDK Command-line tools se necessário
if [ ! -d "$ANDROID_SDK_DIR/cmdline-tools/latest/bin" ]; then
    echo "Baixando Android Command-line Tools..."
    mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
    TMP_ZIP=$(mktemp /tmp/cmdtools_XXXX.zip)
    curl -L "$CMD_TOOLS_URL" -o "$TMP_ZIP"
    unzip -q "$TMP_ZIP" -d "$ANDROID_SDK_DIR/cmdline-tools/"
    mv "$ANDROID_SDK_DIR/cmdline-tools/cmdline-tools" "$ANDROID_SDK_DIR/cmdline-tools/latest"
    rm "$TMP_ZIP"
    echo "✓ Command-line tools instalados em $ANDROID_SDK_DIR"
fi

export ANDROID_HOME="$ANDROID_SDK_DIR"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# 3. Aceitar licenças e instalar plataforma Android 34
echo "Instalando Android SDK platform 34 e build-tools..."
yes | sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"

echo "✓ SDK configurado!"

# 4. Build do APK
echo ""
echo "=== Gerando APK de debug ==="
cd "$(dirname "$0")"
./gradlew assembleDebug

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "✓✓✓ APK gerado com sucesso!"
    echo "   Caminho: $(pwd)/$APK_PATH"
    echo ""
    echo "Instalar via ADB: adb install $APK_PATH"
else
    echo "ERRO: APK não foi gerado. Verifique os logs acima."
fi

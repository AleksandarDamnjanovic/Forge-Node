#!/usr/bin/env bash
set -euo pipefail

# Forge Node Android — Lightweight Command-Line SDK Setup, Build & Phone Installer
# Works 100% from the terminal without Android Studio.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

export JAVA_HOME="${JAVA_HOME:-/home/irishpub/Programming/OpenJDK25U-jdk_x64_linux_hotspot_25.0.4.1_1/jdk-25.0.4.1+1}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "=== Forge Node Android CLI Builder ==="
echo "JAVA_HOME    : $JAVA_HOME"
echo "ANDROID_HOME : $ANDROID_HOME"

# 1. Ensure app icon is copied into res/drawable/ic_forge_node.png
mkdir -p "$SCRIPT_DIR/app/src/main/res/drawable"
if [ ! -f "$SCRIPT_DIR/app/src/main/res/drawable/ic_forge_node.png" ]; then
    if [ -f "$SCRIPT_DIR/../UI/forge-node/src/main/resources/static/Forge Node Studio Icon.png" ]; then
        cp "$SCRIPT_DIR/../UI/forge-node/src/main/resources/static/Forge Node Studio Icon.png" \
           "$SCRIPT_DIR/app/src/main/res/drawable/ic_forge_node.png"
    elif [ -f "$SCRIPT_DIR/../Forge Node Icon.png" ]; then
        cp "$SCRIPT_DIR/../Forge Node Icon.png" \
           "$SCRIPT_DIR/app/src/main/res/drawable/ic_forge_node.png"
    fi
fi

# 2. Install Android Command-Line Tools if sdkmanager is not present
if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo ">>> Downloading official Android Command-Line Tools (~150 MB)..."
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    TMP_ZIP="/tmp/commandlinetools-linux.zip"
    curl -L "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o "$TMP_ZIP"
    unzip -q -o "$TMP_ZIP" -d "$ANDROID_HOME/cmdline-tools"
    rm -rf "$ANDROID_HOME/cmdline-tools/latest"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    rm -f "$TMP_ZIP"
fi

# 3. Accept licenses and install platform-tools (adb), platforms;android-34, build-tools;34.0.0
if [ ! -d "$ANDROID_HOME/platforms/android-34" ] || [ ! -d "$ANDROID_HOME/build-tools/34.0.0" ]; then
    echo ">>> Installing Android SDK Platform 34, Build-Tools 34.0.0 & ADB (platform-tools)..."
    yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true
    "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" \
        "platform-tools" \
        "platforms;android-34" \
        "build-tools;34.0.0"
fi

# Write local.properties for Gradle
echo "sdk.dir=$ANDROID_HOME" > "$SCRIPT_DIR/local.properties"

# 4. Ensure Gradle 9.1.0 binary (supports JDK 25) is available if ./gradlew isn't generated yet
GRADLE_BIN=""
if [ -x "$SCRIPT_DIR/gradlew" ]; then
    GRADLE_BIN="$SCRIPT_DIR/gradlew"
else
    GRADLE_DIST_DIR="$HOME/.gradle/wrapper/dists/gradle-9.1.0-bin"
    if [ ! -x "$GRADLE_DIST_DIR/gradle-9.1.0/bin/gradle" ]; then
        echo ">>> Downloading Gradle 9.1.0 CLI (JDK 25 support)..."
        mkdir -p "$GRADLE_DIST_DIR"
        curl -L "https://services.gradle.org/distributions/gradle-9.1.0-bin.zip" -o "/tmp/gradle-9.1.0-bin.zip"
        unzip -q -o "/tmp/gradle-9.1.0-bin.zip" -d "$GRADLE_DIST_DIR"
        rm -f "/tmp/gradle-9.1.0-bin.zip"
    fi
    GRADLE_BIN="$GRADLE_DIST_DIR/gradle-9.1.0/bin/gradle"
fi

echo ">>> Compiling Debug APK..."
"$GRADLE_BIN" assembleDebug --no-daemon

APK_PATH="$SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "=========================================================="
echo "✓ APK compiled successfully:"
echo "  $APK_PATH"
echo "=========================================================="

if [ "${1:-}" = "--install" ]; then
    echo ">>> Installing APK onto connected Android phone via ADB..."
    "$ANDROID_HOME/platform-tools/adb" install -r "$APK_PATH"
    echo "✓ Installed on phone!"
else
    echo "To install directly to your phone via USB:"
    echo "  ./build-and-install.sh --install"
    echo "Or copy $APK_PATH to your phone and tap it to install."
fi

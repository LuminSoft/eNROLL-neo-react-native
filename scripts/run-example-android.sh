#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# Build and run the eNROLL React Native example app on Android
# ──────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/example-app"
APP_ID="com.luminsoft.EnrollTestingApp"
MAIN_ACTIVITY="com.enrollexample.MainActivity"
METRO_PORT="8081"
METRO_LOG_FILE="/tmp/enroll-neo-example-metro.log"
APK_PATH="$EXAMPLE_DIR/android/app/build/outputs/apk/debug/app-debug.apk"

# Prefer a physical device when one is connected; otherwise fall back to an emulator.
TARGET_SERIAL="$(
  adb devices \
    | awk '/\tdevice$/ {print $1}' \
    | grep -v '^emulator-' \
    | head -n 1
)"

if [ -z "$TARGET_SERIAL" ]; then
  TARGET_SERIAL="$(
    adb devices \
      | awk '/\tdevice$/ {print $1}' \
      | grep '^emulator-' \
      | head -n 1
  )"
fi

if [ -z "$TARGET_SERIAL" ]; then
  echo "No connected Android device or emulator found."
  exit 1
fi

export ANDROID_SERIAL="$TARGET_SERIAL"

echo "==> Using Android target: $TARGET_SERIAL"

echo "==> Installing plugin dependencies..."
cd "$ROOT_DIR"
npm install

echo "==> Installing example app dependencies..."
cd "$EXAMPLE_DIR"
npm install

echo "==> Starting Metro bundler in background..."
lsof -ti:"$METRO_PORT" | xargs kill -9 2>/dev/null || true
rm -f "$METRO_LOG_FILE"
nohup npx react-native start --port "$METRO_PORT" --no-interactive --reset-cache >"$METRO_LOG_FILE" 2>&1 </dev/null &
METRO_PID=$!
disown "$METRO_PID" 2>/dev/null || true

echo "==> Waiting for Metro to be ready..."
for _ in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:$METRO_PORT/status" 2>/dev/null | grep -q "packager-status:running"; then
    break
  fi
  sleep 1
done

if ! curl -fsS "http://127.0.0.1:$METRO_PORT/status" 2>/dev/null | grep -q "packager-status:running"; then
  echo "Metro did not become ready. Check $METRO_LOG_FILE"
  exit 1
fi

echo "==> Building & installing Android app..."
cd "$EXAMPLE_DIR/android"

./gradlew :app:assembleDebug

if [ ! -f "$APK_PATH" ]; then
  echo "APK not found at $APK_PATH"
  exit 1
fi

echo "==> Installing APK on $TARGET_SERIAL..."
if ! adb -s "$TARGET_SERIAL" install -r -d "$APK_PATH"; then
  echo "==> Retrying after uninstall..."
  adb -s "$TARGET_SERIAL" uninstall "$APP_ID" >/dev/null 2>&1 || true
  adb -s "$TARGET_SERIAL" install -r -d "$APK_PATH"
fi

echo "==> Wiring device to Metro on port $METRO_PORT..."
adb -s "$TARGET_SERIAL" reverse "tcp:$METRO_PORT" "tcp:$METRO_PORT"

echo "==> Launching app..."
adb -s "$TARGET_SERIAL" shell am force-stop "$APP_ID" >/dev/null 2>&1 || true
adb -s "$TARGET_SERIAL" shell am start -n "$APP_ID/$MAIN_ACTIVITY"
echo "==> Metro is running in background. Logs: $METRO_LOG_FILE"

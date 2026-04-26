#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# Build and run the eNROLL React Native example app on iOS
# ──────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/example-app"
IOS_DIR="$EXAMPLE_DIR/ios"
WORKSPACE_PATH="$IOS_DIR/EnrollExample.xcworkspace"
SCHEME="EnrollExample"
BUNDLE_ID="com.luminsoft.EnrollTestingApp"
DEVICE_DERIVED_DATA_PATH="$IOS_DIR/build-device"
SIMULATOR_DERIVED_DATA_PATH="$IOS_DIR/build-simulator"
METRO_PORT="8081"
METRO_LOG_FILE="/tmp/enroll-neo-example-ios-metro.log"
DEVELOPMENT_TEAM="$(
  security find-identity -v -p codesigning 2>/dev/null \
    | sed -n 's/.*(\([A-Z0-9]\{10\}\))".*/\1/p' \
    | head -n 1
)"

echo "==> Installing plugin dependencies..."
cd "$ROOT_DIR"
npm install

echo "==> Installing example app dependencies..."
cd "$EXAMPLE_DIR"
npm install

echo "==> Installing CocoaPods..."
cd "$IOS_DIR"
pod install --repo-update

echo "==> Building & launching iOS app..."
cd "$EXAMPLE_DIR"

echo "==> Starting Metro bundler in background..."
lsof -ti:"$METRO_PORT" | xargs kill -9 2>/dev/null || true
rm -f "$METRO_LOG_FILE"
nohup npx react-native start --port "$METRO_PORT" --no-interactive --reset-cache >"$METRO_LOG_FILE" 2>&1 </dev/null &
sleep 3

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

DEVICE_UDID="$(
  xcrun xctrace list devices 2>/dev/null \
    | sed -n 's/^.*(.*) (\([0-9A-F-]\{25,\}\))$/\1/p' \
    | head -n 1
)"

if [ -n "$DEVICE_UDID" ]; then
  if [ -z "$DEVELOPMENT_TEAM" ]; then
    echo "No Apple Development signing identity was found for building to a physical iPhone."
    exit 1
  fi

  echo "==> Building for connected iPhone: $DEVICE_UDID"
  rm -rf "$DEVICE_DERIVED_DATA_PATH"
  xcodebuild \
    -workspace "$WORKSPACE_PATH" \
    -scheme "$SCHEME" \
    -configuration Debug \
    -destination "id=$DEVICE_UDID" \
    -derivedDataPath "$DEVICE_DERIVED_DATA_PATH" \
    -allowProvisioningUpdates \
    -allowProvisioningDeviceRegistration \
    DEVELOPMENT_TEAM="$DEVELOPMENT_TEAM" \
    CODE_SIGN_STYLE=Automatic \
    build

  DEVICE_APP_PATH="$DEVICE_DERIVED_DATA_PATH/Build/Products/Debug-iphoneos/$SCHEME.app"

  if [ ! -d "$DEVICE_APP_PATH" ]; then
    echo "Built app not found at $DEVICE_APP_PATH"
    exit 1
  fi

  echo "==> Installing app on iPhone..."
  xcrun devicectl device uninstall app --device "$DEVICE_UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true
  xcrun devicectl device install app --device "$DEVICE_UDID" "$DEVICE_APP_PATH"

  echo "==> Launching app on iPhone..."
  xcrun devicectl device process launch --device "$DEVICE_UDID" --terminate-existing "$BUNDLE_ID"
else
  SIMULATOR_UDID="$(
    xcrun simctl list devices available \
      | sed -n 's/^[[:space:]]*iPhone[^()]* (\([A-F0-9-]\+\)) (.*$/\1/p' \
      | head -n 1
  )"

  if [ -z "$SIMULATOR_UDID" ]; then
    echo "No connected iPhone found, and no available simulator was detected."
    exit 1
  fi

  echo "==> Booting simulator: $SIMULATOR_UDID"
  open -a Simulator --args -CurrentDeviceUDID "$SIMULATOR_UDID"
  xcrun simctl boot "$SIMULATOR_UDID" 2>/dev/null || true
  xcrun simctl bootstatus "$SIMULATOR_UDID" -b

  echo "==> Building for simulator..."
  rm -rf "$SIMULATOR_DERIVED_DATA_PATH"
  xcodebuild \
    -workspace "$WORKSPACE_PATH" \
    -scheme "$SCHEME" \
    -configuration Debug \
    -destination "id=$SIMULATOR_UDID" \
    -derivedDataPath "$SIMULATOR_DERIVED_DATA_PATH" \
    build

  SIMULATOR_APP_PATH="$SIMULATOR_DERIVED_DATA_PATH/Build/Products/Debug-iphonesimulator/$SCHEME.app"

  if [ ! -d "$SIMULATOR_APP_PATH" ]; then
    echo "Built app not found at $SIMULATOR_APP_PATH"
    exit 1
  fi

  echo "==> Installing app on simulator..."
  xcrun simctl install "$SIMULATOR_UDID" "$SIMULATOR_APP_PATH"

  echo "==> Launching app on simulator..."
  xcrun simctl launch "$SIMULATOR_UDID" "$BUNDLE_ID"
fi

echo "==> Metro is running in background. Logs: $METRO_LOG_FILE"

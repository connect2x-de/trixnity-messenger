#!/usr/bin/env bash
set -euo pipefail

DEVICE_NAME="${DEVICE_NAME:-iPhone 16}"
IOS_VERSION="${IOS_VERSION:-latest}"

echo "🔍 Finding simulator UDID for: $DEVICE_NAME ($IOS_VERSION)"

# Get the UDID of the desired simulator
UDID=$(xcrun simctl list devices available | grep "$DEVICE_NAME" | head -n 1 | awk -F '[()]' '{print $2}')

if [ -z "$UDID" ]; then
  echo "❌ Could not find simulator for $DEVICE_NAME"
  exit 1
fi

echo "📱 Using simulator UDID: $UDID"

# Boot the simulator (no-op if already booted)
echo "🚀 Booting simulator..."
xcrun simctl boot "$UDID" || true

# Wait until booted state
echo "⏳ Waiting for device to be booted..."
xcrun simctl bootstatus "$UDID" -b

echo "🎉 Simulator is ready for UI tests"

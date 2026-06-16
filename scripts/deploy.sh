#!/usr/bin/env bash
# Deploy portalani to a Meta Portal:
#   install APK -> grant screensaver permission -> register dream service -> launch
#
# Usage:
#   scripts/deploy.sh [-s SERIAL] [--apk PATH] [--build]
set -euo pipefail

PKG="com.portal.portalani"
DREAM="com.portal.portalani/com.portal.portalani.AnimeDreamService"

SERIAL="${SERIAL:-}"
if [[ -n "${APK:-}" ]]; then
  :
elif [[ -f "app/build/outputs/apk/release/app-release.apk" ]]; then
  APK="app/build/outputs/apk/release/app-release.apk"
else
  APK="app/build/outputs/apk/debug/app-debug.apk"
fi
DO_BUILD=0

usage() { sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s|--serial) SERIAL="$2"; shift 2;;
    --apk)       APK="$2"; shift 2;;
    --build)     DO_BUILD=1; shift;;
    -h|--help)   usage; exit 0;;
    *) echo "unknown arg: $1" >&2; usage >&2; exit 1;;
  esac
done

ADB="${ADB:-}"
if [[ -z "$ADB" ]]; then
  if command -v adb >/dev/null 2>&1; then ADB="$(command -v adb)"
  elif [[ -n "${ANDROID_HOME:-}" && -x "$ANDROID_HOME/platform-tools/adb" ]]; then ADB="$ANDROID_HOME/platform-tools/adb"
  elif [[ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]]; then ADB="$HOME/Library/Android/sdk/platform-tools/adb"
  else echo "adb not found — set ADB or ANDROID_HOME" >&2; exit 1; fi
fi

if [[ -z "$SERIAL" ]]; then
  DEVICES=()
  while IFS= read -r line; do
    [[ -n "$line" ]] && DEVICES+=("$line")
  done < <("$ADB" devices | awk 'NR>1 && $2=="device"{print $1}')
  case ${#DEVICES[@]} in
    0) echo "no authorized adb devices" >&2; exit 1;;
    1) SERIAL="${DEVICES[0]}";;
    *) echo "multiple devices; pass -s SERIAL. Found: ${DEVICES[*]}" >&2; exit 1;;
  esac
fi
adb() { "$ADB" -s "$SERIAL" "$@"; }
echo ">> device: $SERIAL"

if [[ $DO_BUILD -eq 1 ]]; then
  echo ">> ./gradlew assembleDebug"
  ./gradlew assembleDebug
elif [[ ! -f "$APK" ]]; then
  echo ">> APK missing — ./gradlew assembleDebug"
  ./gradlew assembleDebug
elif [[ "app/build.gradle.kts" -nt "$APK" ]] || find app/src -type f -newer "$APK" -print -quit | grep -q .; then
  echo ">> APK older than sources — ./gradlew assembleDebug"
  ./gradlew assembleDebug
fi
[[ -f "$APK" ]] || { echo "APK not found: $APK (run with --build)" >&2; exit 1; }

echo ">> install $APK"
if adb install -r "$APK" 2>&1 | tee /tmp/portalani-install.log | grep -q "Success"; then
  echo "   ok"
else
  if grep -qiE "INSTALL_FAILED_UPDATE_INCOMPATIBLE|signatures do not match" /tmp/portalani-install.log; then
    echo "   signature mismatch — reinstalling (settings/tokens in app storage may be lost)"
    adb uninstall "$PKG" >/dev/null || true
    adb install "$APK"
  else
    cat /tmp/portalani-install.log >&2
    exit 1
  fi
fi

echo ">> grant WRITE_SECURE_SETTINGS"
adb shell pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS \
  || echo "   failed (screensaver registration may not stick)"

echo ">> register screensaver"
adb shell settings put secure screensaver_components "$DREAM"
adb shell settings put secure screensaver_enabled 1
adb shell settings put secure screensaver_activate_on_sleep 1
CURRENT="$(adb shell settings get secure screensaver_components 2>/dev/null | tr -d '\r')"
echo "   screensaver_components=$CURRENT"

echo ">> launch"
adb shell am start -n "$PKG/.MainActivity" >/dev/null
echo ">> done"

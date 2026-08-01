#!/usr/bin/env bash
set -Eeuo pipefail

PACKAGE="eu.basicairdata.graziano.gpslogger.dev"
LABEL="manual"
OUTPUT_ROOT="baseline-results"
MODE="collect"
ADB_BIN="${ADB:-adb}"

usage() {
  cat <<'EOF'
Usage:
  collect-android-baseline.sh [options]

Options:
  --prepare             Clear logcat and reset batterystats before a scenario.
  --collect             Collect metrics after or during a scenario (default).
  --package <id>        Android package ID.
  --label <name>        Scenario label used in the output folder.
  --output <directory>  Root output directory (default: baseline-results).
  -h, --help            Show this help.

Examples:
  ./scripts/collect-android-baseline.sh --prepare --label walking-60m
  ./scripts/collect-android-baseline.sh --collect --label walking-60m
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --prepare)
      MODE="prepare"
      shift
      ;;
    --collect)
      MODE="collect"
      shift
      ;;
    --package)
      PACKAGE="${2:?Missing value for --package}"
      shift 2
      ;;
    --label)
      LABEL="${2:?Missing value for --label}"
      shift 2
      ;;
    --output)
      OUTPUT_ROOT="${2:?Missing value for --output}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if ! command -v "$ADB_BIN" >/dev/null 2>&1; then
  echo "adb was not found. Install Android Platform Tools or set ADB=/path/to/adb." >&2
  exit 1
fi

"$ADB_BIN" start-server >/dev/null

DEVICE_STATE="$($ADB_BIN get-state 2>/dev/null || true)"
if [[ "$DEVICE_STATE" != "device" ]]; then
  echo "No authorized Android device is available through adb." >&2
  "$ADB_BIN" devices -l >&2 || true
  exit 1
fi

SAFE_LABEL="$(printf '%s' "$LABEL" | tr -cs '[:alnum:]._-' '-')"
SAFE_LABEL="${SAFE_LABEL#-}"
SAFE_LABEL="${SAFE_LABEL%-}"
[[ -n "$SAFE_LABEL" ]] || SAFE_LABEL="manual"

TIMESTAMP="$(date -u +'%Y%m%dT%H%M%SZ')"
OUT_DIR="$OUTPUT_ROOT/${TIMESTAMP}-${SAFE_LABEL}"
mkdir -p "$OUT_DIR"

capture_host() {
  local filename="$1"
  shift
  "$@" >"$OUT_DIR/$filename" 2>&1 || true
}

capture_shell() {
  local filename="$1"
  shift
  "$ADB_BIN" shell "$@" >"$OUT_DIR/$filename" 2>&1 || true
}

SERIAL="$($ADB_BIN get-serialno | tr -d '\r')"

{
  echo "timestamp_utc=$TIMESTAMP"
  echo "mode=$MODE"
  echo "label=$LABEL"
  echo "package=$PACKAGE"
  echo "serial=$SERIAL"
  echo "device_state=$DEVICE_STATE"
} >"$OUT_DIR/session.properties"

capture_host "adb-version.txt" "$ADB_BIN" version
capture_host "adb-devices.txt" "$ADB_BIN" devices -l
capture_shell "device-properties.txt" getprop

if [[ "$MODE" == "prepare" ]]; then
  "$ADB_BIN" logcat -c
  "$ADB_BIN" shell dumpsys batterystats --reset >/dev/null
  {
    echo "Baseline preparation completed."
    echo "Logcat was cleared and batterystats were reset."
    echo "Start the scenario in GPS Logger Dev, then run this script with --collect."
  } >"$OUT_DIR/README.txt"
  echo "Prepared baseline session: $OUT_DIR"
  exit 0
fi

PACKAGE_PATH="$($ADB_BIN shell pm path "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [[ "$PACKAGE_PATH" != package:* ]]; then
  echo "Package $PACKAGE is not installed on the connected device." >&2
  exit 1
fi

echo "package_path=$PACKAGE_PATH" >>"$OUT_DIR/session.properties"

PACKAGE_DUMP="$OUT_DIR/package.txt"
"$ADB_BIN" shell dumpsys package "$PACKAGE" >"$PACKAGE_DUMP" 2>&1 || true

PID_RAW="$($ADB_BIN shell pidof "$PACKAGE" 2>/dev/null || true)"
PID="$(printf '%s' "$PID_RAW" | tr -d '\r' | awk '{print $1}')"
{
  echo "pid=${PID:-not-running}"
  grep -E 'versionName=|versionCode=|firstInstallTime=|lastUpdateTime=' "$PACKAGE_DUMP" || true
} >>"$OUT_DIR/session.properties"

capture_shell "meminfo.txt" dumpsys meminfo "$PACKAGE"
capture_shell "cpuinfo.txt" dumpsys cpuinfo
capture_shell "batterystats.txt" dumpsys batterystats "$PACKAGE"
capture_shell "procstats.txt" dumpsys procstats --hours 3 "$PACKAGE"
capture_shell "gfxinfo.txt" dumpsys gfxinfo "$PACKAGE" framestats
capture_shell "appops.txt" cmd appops get "$PACKAGE"
capture_shell "services.txt" dumpsys activity services "$PACKAGE"
capture_shell "processes.txt" dumpsys activity processes "$PACKAGE"
capture_shell "location.txt" dumpsys location
capture_shell "power.txt" dumpsys power
capture_shell "device-idle.txt" dumpsys deviceidle
capture_shell "battery.txt" dumpsys battery
capture_shell "thermal-status.txt" dumpsys thermalservice

if [[ -n "$PID" ]]; then
  capture_host "logcat.txt" "$ADB_BIN" logcat -d --pid="$PID" -v threadtime
else
  "$ADB_BIN" logcat -d -v threadtime 2>&1 \
    | grep -F "$PACKAGE" >"$OUT_DIR/logcat.txt" || true
fi

{
  echo "GPS Logger optimization baseline"
  echo
  echo "Scenario: $LABEL"
  echo "Package: $PACKAGE"
  echo "Device: $SERIAL"
  echo "Captured: $TIMESTAMP"
  echo "PID: ${PID:-not-running}"
  echo
  echo "Important files:"
  echo "- meminfo.txt: Java/native heap and process memory"
  echo "- cpuinfo.txt: current CPU use"
  echo "- batterystats.txt: battery attribution since the last reset"
  echo "- procstats.txt: process residency over the last three hours"
  echo "- services.txt: foreground/background service state"
  echo "- location.txt: active location requests and providers"
  echo "- logcat.txt: application log output"
} >"$OUT_DIR/README.txt"

echo "Baseline captured in: $OUT_DIR"

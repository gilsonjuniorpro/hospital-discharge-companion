#!/usr/bin/env bash
set -euo pipefail

APP_ID="com.hospitaldischargecompanion"
OUT_DIR="$(cd "$(dirname "$0")/.." && pwd)/docs/screenshots"
APK_PATH="$(cd "$(dirname "$0")/.." && pwd)/app/build/outputs/apk/debug/app-debug.apk"

mkdir -p "$OUT_DIR"

if ! adb get-state >/dev/null 2>&1; then
  echo "No connected Android device or running emulator detected." >&2
  exit 1
fi

if [[ -f "$APK_PATH" ]]; then
  adb install -r "$APK_PATH" >/dev/null
fi

capture_screen() {
  local route_slug="$1"
  local file_name="$2"
  echo "Capturing $file_name ($route_slug)..."
  adb shell am start -W -a android.intent.action.VIEW -d "hdc://screen/${route_slug}" -n "${APP_ID}/.MainActivity" >/dev/null
  sleep 4
  adb exec-out screencap -p > "${OUT_DIR}/${file_name}"
}

adb shell am force-stop "$APP_ID"
adb shell am start -W -n "${APP_ID}/.MainActivity" >/dev/null
sleep 3
adb exec-out screencap -p > "${OUT_DIR}/01_profile_selection.png"

capture_screen "patient/ep-hf-001" "02_patient_home.png"
capture_screen "family/ep-hf-001" "03_family_home.png"
capture_screen "nurse" "04_nurse_workstation.png"
capture_screen "physician" "05_physician_workstation.png"
capture_screen "careplan/ep-hf-001" "06_care_plan.png"
capture_screen "medications/ep-hf-001" "07_medications.png"
capture_screen "telemonitoring/ep-hf-001" "08_telemonitoring.png"
capture_screen "alerts/ep-hf-001" "09_alerts.png"
capture_screen "messaging/ep-hf-001" "10_messaging.png"
capture_screen "appointments/ep-hf-001" "11_appointments.png"
capture_screen "education/ep-hf-001" "12_education.png"
capture_screen "ehr/ep-hf-001" "13_ehr_sync.png"
capture_screen "demo" "14_demo_controls.png"

echo "Saved all 14 screenshots to ${OUT_DIR}"

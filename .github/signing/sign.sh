#!/usr/bin/env bash
# Signs an APK with key rotation: the old (public) key -> the new private key.
# Android 9+ moves to the new key without a reinstall; Android 7/8 keep trusting the old key.
# Usage: sign.sh <in.apk> <out.apk>     needs env NEW_KEY_PASS and the old keystore in the app folder
set -euo pipefail
IN="$1"; OUT="$2"
HERE="$(cd "$(dirname "$0")" && pwd)"
OLD="${OLD_KEYSTORE:-plafond-led-app/app/release.keystore}"
APKSIGNER="$(ls -d "$ANDROID_HOME"/build-tools/*/ | sort -V | tail -1)apksigner"
WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT
openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 -in "$HERE/new-key.jks.enc" -out "$WORK/new.jks" -pass env:NEW_KEY_PASS
"$APKSIGNER" rotate --out "$WORK/lineage" \
  --old-signer --ks "$OLD" --ks-pass pass:plafondled --ks-key-alias plafondled \
  --new-signer --ks "$WORK/new.jks" --ks-pass env:NEW_KEY_PASS --ks-key-alias tasmotascan
"$APKSIGNER" sign --ks "$OLD" --ks-pass pass:plafondled --ks-key-alias plafondled \
  --next-signer --ks "$WORK/new.jks" --ks-pass env:NEW_KEY_PASS --ks-key-alias tasmotascan \
  --lineage "$WORK/lineage" --rotation-min-sdk-version 28 --out "$OUT" "$IN"
"$APKSIGNER" verify --verbose --print-certs "$OUT" | grep -E "Verified using|Signer|SHA-256|rotation|lineage" || true

#!/usr/bin/env bash
set -euo pipefail

# Generates a refined transparent foreground from the existing drawable/logo_app.png
# and then calls replace_icon.sh to create mipmaps.

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT_DIR/app/src/main/res/drawable/logo_app.png"
TMP_DIR="$(mktemp -d)"

if [[ ! -f "$SRC" ]]; then
  echo "Source not found: $SRC"
  exit 2
fi

cmd=""
if command -v magick >/dev/null 2>&1; then
  cmd="magick"
elif command -v convert >/dev/null 2>&1; then
  cmd="convert"
else
  echo "ImageMagick not found (magick or convert). Install it and retry." >&2
  exit 3
fi

echo "Creating refined foreground from $SRC"

# sample top-left pixel for background color
bghex=$($cmd "$SRC" -format "%[hex:p{0,0}]" info:- 2>/dev/null || true)
if [[ -z "$bghex" ]]; then
  bghex=$($cmd "$SRC" -format "%[hex:p{10,10}]" info:-)
fi
bg="#$bghex"
echo "Detected background color: $bg"

TMP1="$TMP_DIR/fg_transparent.png"
TMP2="$TMP_DIR/fg_resized.png"
OUT="$ROOT_DIR/app/src/main/res/drawable/logo_app.png"

# make background transparent with fuzz
$cmd "$SRC" -fuzz 10% -transparent "$bg" "$TMP1"

# resize and center on larger canvas for padding
$cmd "$TMP1" -resize 820x820 -background none -gravity center -extent 1024x1024 "$TMP2"

# add soft shadow: create shadow then composite under the logo
$cmd "$TMP2" \( +clone -background black -alpha extract -blur 0x12 -shade 120x45 -evaluate multiply 0.6 -background none -alpha set -channel A -evaluate set 50% +channel \) -reverse -compose over -flatten "$OUT"

echo "Refined foreground written to $OUT"

chmod +x "$ROOT_DIR/scripts/replace_icon.sh" || true
"$ROOT_DIR/scripts/replace_icon.sh" "$OUT"

rm -rf "$TMP_DIR"

echo "Done."

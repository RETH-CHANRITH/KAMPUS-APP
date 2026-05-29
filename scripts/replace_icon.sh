#!/usr/bin/env bash
set -euo pipefail

# Simple icon replacement: generate mipmap PNGs and WebP files for densities.
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$1"
if [[ ! -f "$SRC" ]]; then
  echo "Source not found: $SRC"
  exit 2
fi

if command -v magick >/dev/null 2>&1; then
  cmd="magick"
elif command -v convert >/dev/null 2>&1; then
  cmd="convert"
else
  echo "ImageMagick not found. Install it and retry." >&2
  exit 3
fi

RES_DIR="$ROOT_DIR/app/src/main/res"
declare -a dirs=(mipmap-mdpi mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi)
declare -a sizes=(48 72 96 144 192)

for i in "${!dirs[@]}"; do
  dir=${dirs[$i]}
  size=${sizes[$i]}
  outdir="$RES_DIR/$dir"
  mkdir -p "$outdir"
  png_out="$outdir/ic_launcher.png"
  fg_out="$outdir/ic_launcher_foreground.png"
  round_out="$outdir/ic_launcher_round.png"
  echo "Generating $dir @ ${size}"
  $cmd "$SRC" -resize ${size}x${size} -background none -gravity center -extent ${size}x${size} "$png_out"
  $cmd "$SRC" -resize ${size}x${size} -background none -gravity center -extent ${size}x${size} "$fg_out"
  $cmd "$SRC" -resize ${size}x${size} -background none -gravity center -extent ${size}x${size} "$round_out"
  # convert png to webp to keep repo small
  cwebp -q 90 "$png_out" -o "$outdir/ic_launcher.webp" >/dev/null 2>&1 || true
  cwebp -q 90 "$fg_out" -o "$outdir/ic_launcher_foreground.webp" >/dev/null 2>&1 || true
  cwebp -q 90 "$round_out" -o "$outdir/ic_launcher_round.webp" >/dev/null 2>&1 || true
done

# copy drawable foreground
mkdir -p "$RES_DIR/drawable"
cp "$SRC" "$RES_DIR/drawable/logo_app.png"

echo "Icons generated."

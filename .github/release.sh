#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# release.sh -- build a release candidate zip for LastActivePlayers
#
# Output: release/LastActivePlayers-<version>.zip
#   ├── LastActivePlayers-<version>.jar   (fat jar -- drop into plugins/)
#   ├── README.md
#   └── CHANGELOG.md
# ---------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

# --- Build ---
echo "Building fat jar..."
set +u; source "$HOME/.sdkman/bin/sdkman-init.sh" 2>/dev/null || true; set -u
./gradlew shadowJar --quiet

# --- Determine version from the built jar ---
JAR=$(ls build/libs/*.jar | head -1)
FILENAME=$(basename "$JAR")                             # minecraft-last-active-players-1.0.0-SNAPSHOT.jar
VERSION="${FILENAME#minecraft-last-active-players-}"   # 1.0.0-SNAPSHOT.jar
VERSION="${VERSION%.jar}"                               # 1.0.0-SNAPSHOT

RELEASE_NAME="LastActivePlayers-${VERSION}"
RELEASE_DIR="release/${RELEASE_NAME}"
ZIP_PATH="release/${RELEASE_NAME}.zip"

# --- Stage files ---
rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR"

cp "$JAR"        "$RELEASE_DIR/${RELEASE_NAME}.jar"
cp README.md     "$RELEASE_DIR/README.md"
cp CHANGELOG.md  "$RELEASE_DIR/CHANGELOG.md"

# --- Zip ---
rm -f "$ZIP_PATH"
(cd release && zip -r "$(basename "$ZIP_PATH")" "$(basename "$RELEASE_DIR")")
rm -rf "$RELEASE_DIR"

echo ""
echo "Release candidate ready: $ZIP_PATH"
echo ""
unzip -l "$ZIP_PATH"

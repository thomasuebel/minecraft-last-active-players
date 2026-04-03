#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# do-release.sh -- cut a release from master
#
# Usage: .github/do-release.sh
#
# Workflow:
#   1. Validate: on master, clean working tree, current version is a SNAPSHOT
#   2. Strip -SNAPSHOT  ->  release version  (e.g. 1.0.1-SNAPSHOT -> 1.0.1)
#   3. Commit: "chore: release 1.0.1"
#   4. Tag:    v1.0.1
#   5. Bump patch + restore -SNAPSHOT  (1.0.1 -> 1.0.2-SNAPSHOT)
#   6. Commit: "chore: bump to 1.0.2-SNAPSHOT"
#   7. Push commits and tag  ->  CI picks up the tag and publishes the release
# ---------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

# --- Preconditions ---
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ "$BRANCH" != "master" ]]; then
    echo "ERROR: must be on master (currently on '$BRANCH')" >&2
    exit 1
fi

git fetch --quiet origin master
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse origin/master)
if [[ "$LOCAL" != "$REMOTE" ]]; then
    echo "ERROR: master is not up to date with origin/master -- pull first" >&2
    exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "ERROR: working tree is not clean -- commit or stash changes first" >&2
    exit 1
fi

# --- Read and validate current version ---
CURRENT=$(grep '^version = ' build.gradle.kts | perl -pe 's/version = ["'"'"'](.*)["'"'"']/\1/')
if [[ "$CURRENT" != *-SNAPSHOT ]]; then
    echo "ERROR: current version '$CURRENT' is not a SNAPSHOT -- nothing to release" >&2
    exit 1
fi

RELEASE="${CURRENT%-SNAPSHOT}"

MAJOR="${RELEASE%%.*}"
REST="${RELEASE#*.}"
MINOR="${REST%.*}"
PATCH="${REST##*.}"
NEXT="${MAJOR}.${MINOR}.$((PATCH + 1))-SNAPSHOT"

echo "Current:       $CURRENT"
echo "Release:       $RELEASE"
echo "Next snapshot: $NEXT"
echo ""

# --- Release commit ---
perl -pi -e "s/version = \"${CURRENT}\"/version = \"${RELEASE}\"/" build.gradle.kts
git add build.gradle.kts
git commit -m "chore: release ${RELEASE}"
git tag "v${RELEASE}"
echo "Tagged v${RELEASE}"

# --- Next-snapshot commit ---
perl -pi -e "s/version = \"${RELEASE}\"/version = \"${NEXT}\"/" build.gradle.kts
git add build.gradle.kts
git commit -m "chore: bump to ${NEXT}"
echo "Bumped to ${NEXT}"

# --- Push ---
git push origin master
git push origin "v${RELEASE}"

echo ""
echo "Done -- CI will build and publish the release for v${RELEASE}"

#!/usr/bin/env bash
# Quay ve phien ban truoc do (previous_tag) cua moi truong chua script nay.
set -euo pipefail
cd "$(dirname "$(readlink -f "$0")")"
PREV_TAG="$(cat previous_tag 2>/dev/null || true)"
[[ -n "$PREV_TAG" ]] || { echo "Khong co previous_tag de rollback" >&2; exit 2; }
echo "Rollback ve $PREV_TAG"
exec ./deploy.sh "$PREV_TAG"

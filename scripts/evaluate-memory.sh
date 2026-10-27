#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mode="${1:-offline}"
# This command never activates the separate single-call LiveModelIT.
export MIRROR_RUN_LIVE_LLM=false
case "$mode" in
  offline) export MIRROR_EVALUATION_LIVE_ENABLED=false; test_class=MemoryEvaluationOfflineIT ;;
  rules) export MIRROR_EVALUATION_LIVE_ENABLED=false; test_class=MemoryEvaluationRuleIT ;;
  live)
    if [[ "${MIRROR_EVALUATION_LIVE_ENABLED:-false}" != true || "${MIRROR_MODEL_EGRESS_ENABLED:-false}" != true ]]; then
      echo 'Live evaluation requires explicit evaluation consent and model egress permission.' >&2; exit 2
    fi
    if [[ -z "${MIRROR_LLM_API_KEY:-}" || -z "${MIRROR_LLM_MODEL:-}" ]]; then
      echo 'Configure the model identifier and inject credentials through your secure environment.' >&2; exit 2
    fi
    dataset="${MIRROR_EVALUATION_DATASET:-evaluation/datasets/memory-quality-v1.json}"
    if command -v sha256sum >/dev/null 2>&1; then
      digest="$(sha256sum "$dataset" | cut -d ' ' -f1)"
    else
      digest="$(shasum -a 256 "$dataset" | cut -d ' ' -f1)"
    fi
    if [[ "${MIRROR_EVALUATION_APPROVED_DATASET_SHA256:-}" != "$digest" ]]; then
      echo 'Review the fictional dataset first, then explicitly supply its SHA-256 approval.' >&2; exit 2
    fi
    test_class=MemoryEvaluationLiveIT ;;
  *) echo 'Usage: bash scripts/evaluate-memory.sh [offline|rules|live] [Maven options...]' >&2; exit 2 ;;
esac
if [[ $# -gt 0 ]]; then shift; fi
if ! mvn -v | grep -Eq 'Java version: 21([., ]|$)'; then
  echo 'Run Maven with Java 21 (check JAVA_HOME and mvn -v).' >&2; exit 2
fi
exec mvn -q "-Dit.test=$test_class" \
  "-Devaluation.dataset=${MIRROR_EVALUATION_DATASET:-evaluation/datasets/memory-quality-v1.json}" verify "$@"

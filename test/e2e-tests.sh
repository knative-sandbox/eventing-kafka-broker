#!/usr/bin/env bash

# variables used:
# - SKIP_INITIALIZE (default: false) - skip cluster creation.
# - LOCAL_DEVELOPMENT (default: false) - skip heavy workloads installation like load and chaos generators.

readonly SKIP_INITIALIZE=${SKIP_INITIALIZE:-false}
readonly LOCAL_DEVELOPMENT=${LOCAL_DEVELOPMENT:-false}
export REPLICAS=${REPLICAS:-3}

ROOT_DIR=$(dirname $0)/..

source $(dirname $0)/e2e-common.sh

# If gcloud is not available make it a no-op, not an error.
which gcloud &>/dev/null || gcloud() { echo "[ignore-gcloud $*]" 1>&2; }

# Use GNU tools on macOS. Requires the 'grep' and 'gnu-sed' Homebrew formulae.
if [ "$(uname)" == "Darwin" ]; then
  sed=gsed
  grep=ggrep
fi

if ! ${SKIP_INITIALIZE}; then
  initialize $@ --skip-istio-addon
fi

save_release_artifacts || fail_test "Failed to save release artifacts"

if ! ${LOCAL_DEVELOPMENT}; then
  "${ROOT_DIR}"/hack/run deploy-sacura || fail_test "Failed to apply Sacura"
  "${ROOT_DIR}"/hack/run deploy-chaos || fail_test "Failed to apply chaos"
fi

header "Waiting Knative eventing to come up"

wait_until_pods_running knative-eventing || fail_test "Pods in knative-eventing didn't come up"

header "Running tests"

export_logs_continuously "kafka-broker-dispatcher" "kafka-broker-receiver" "kafka-sink-receiver"

"${ROOT_DIR}"/hack/run.sh integration-tests || fail_test "Integration tests failed"
"${ROOT_DIR}"/hack/run.sh sacura-tests  || fail_test "Sacura test failed"

success

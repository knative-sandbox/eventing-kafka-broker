#!/usr/bin/env bash

set -Eeo pipefail

source $(dirname $0)/e2e-common.sh

# If gcloud is not available make it a no-op, not an error.
which gcloud &>/dev/null || gcloud() { echo "[ignore-gcloud $*]" 1>&2; }

# Use GNU tools on macOS. Requires the 'grep' and 'gnu-sed' Homebrew formulae.
if [ "$(uname)" == "Darwin" ]; then
  sed=gsed
  grep=ggrep
fi

function test_setup() {
  # Override test setup
  return 0
}

function test_teardown() {
  # Override test setup
  return 0
}

if ! ${SKIP_INITIALIZE}; then
  initialize $@ --skip-istio-addon
fi

install_latest_release || fail_test "Failed to apply latest release"

wait_until_pods_running knative-eventing || fail_test "System did not come up"

# Continuous tests
apply_sacura || fail_test "Failed to apply Sacura"

# Upgrade
install_head || fail_test "Failed to apply head"

# Run tests, without waiting for pods to be ready, so that we run them while we're upgrading
# TODO use new conformance tests
go_test_e2e -timeout=30m -short ./test/e2e/conformance/... || fail_test "E2E suite failed (after downgrade)"

# Downgrade
install_latest_release || fail_test "Failed to downgrade to the latest release"

# Run tests, without waiting for pods to be ready, so that we run them while we're upgrading
# TODO use new conformance tests
go_test_e2e -timeout=30m -short ./test/e2e/conformance/... || fail_test "E2E suite failed (after downgrade)"

# Check continuous tests
go_test_e2e -tags=sacura -timeout=40m ./test/e2e/... || fail_test "E2E (sacura) suite failed"

success

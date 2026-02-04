#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashLogging.sh
source src/main/resources/io/viash/helpers/bashutils/ViashCleanupRegistry.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh

# Disable the global EXIT trap for testing purposes
# We'll test the functions directly instead
trap - EXIT

## TEST1: test registering cleanup handlers

# TEST1a: Register a single handler
VIASH_CLEANUP_HANDLERS=()
ViashRegisterCleanup "handler1"
assert_value_equal "test1a_count" "1" "${#VIASH_CLEANUP_HANDLERS[@]}"
assert_value_equal "test1a_handler" "handler1" "${VIASH_CLEANUP_HANDLERS[0]}"

# TEST1b: Register multiple handlers
VIASH_CLEANUP_HANDLERS=()
ViashRegisterCleanup "handler1"
ViashRegisterCleanup "handler2"
ViashRegisterCleanup "handler3"
assert_value_equal "test1b_count" "3" "${#VIASH_CLEANUP_HANDLERS[@]}"
assert_value_equal "test1b_handler0" "handler1" "${VIASH_CLEANUP_HANDLERS[0]}"
assert_value_equal "test1b_handler1" "handler2" "${VIASH_CLEANUP_HANDLERS[1]}"
assert_value_equal "test1b_handler2" "handler3" "${VIASH_CLEANUP_HANDLERS[2]}"


## TEST2: test running cleanup handlers

# TEST2a: Run handlers in reverse order (LIFO)
VIASH_CLEANUP_HANDLERS=()
CLEANUP_ORDER=""
function test_handler1 { CLEANUP_ORDER="${CLEANUP_ORDER}1"; }
function test_handler2 { CLEANUP_ORDER="${CLEANUP_ORDER}2"; }
function test_handler3 { CLEANUP_ORDER="${CLEANUP_ORDER}3"; }
ViashRegisterCleanup "test_handler1"
ViashRegisterCleanup "test_handler2"
ViashRegisterCleanup "test_handler3"
ViashRunCleanupHandlers
assert_value_equal "test2a_order" "321" "$CLEANUP_ORDER"

# TEST2b: Skip non-existent handlers gracefully
VIASH_CLEANUP_HANDLERS=()
CLEANUP_ORDER=""
function test_handler_exists { CLEANUP_ORDER="${CLEANUP_ORDER}E"; }
ViashRegisterCleanup "test_handler_exists"
ViashRegisterCleanup "test_handler_nonexistent"
ViashRunCleanupHandlers
assert_value_equal "test2b_order" "E" "$CLEANUP_ORDER"

# TEST2c: Empty handler list runs without error
VIASH_CLEANUP_HANDLERS=()
ViashRunCleanupHandlers
# If we get here without error, the test passes


## TEST3: test cleanup handlers can access variables from their definition context

# TEST3a: Handler can access variable captured at definition time
VIASH_CLEANUP_HANDLERS=()
CAPTURED_VALUE=""
MY_VAR="original_value"
eval 'function test_capture_handler { CAPTURED_VALUE="$MY_VAR"; }'
ViashRegisterCleanup "test_capture_handler"
MY_VAR="changed_value"
ViashRunCleanupHandlers
assert_value_equal "test3a_capture" "changed_value" "$CAPTURED_VALUE"

# TEST3b: Handler works with local-like pattern (store value at registration time)
VIASH_CLEANUP_HANDLERS=()
RESULT_VALUE=""
WORK_DIR="/tmp/test_workdir_123"
# Create handler that captures current value
eval "function test_workdir_handler { RESULT_VALUE=\"$WORK_DIR\"; }"
ViashRegisterCleanup "test_workdir_handler"
WORK_DIR="/remapped/path"  # Simulate docker remapping
ViashRunCleanupHandlers
assert_value_equal "test3b_stored" "/tmp/test_workdir_123" "$RESULT_VALUE"


## TEST4: test real-world usage pattern (simulating BashWrapper + ExecutableRunner)

# TEST4a: Multiple cleanup handlers from different sources
VIASH_CLEANUP_HANDLERS=()
CLEANUP_LOG=""

# Simulate BashWrapper's work directory cleanup
VIASH_WORK_DIR_ORIGINAL="/tmp/workdir"
function ViashCleanupWorkDir {
  CLEANUP_LOG="${CLEANUP_LOG}[workdir:$VIASH_WORK_DIR_ORIGINAL]"
}
ViashRegisterCleanup ViashCleanupWorkDir

# Simulate ExecutableRunner's chown cleanup
function ViashPerformChown {
  CLEANUP_LOG="${CLEANUP_LOG}[chown]"
}
ViashRegisterCleanup ViashPerformChown

# Run all handlers
ViashRunCleanupHandlers

# Chown should run first (LIFO), then workdir cleanup
assert_value_equal "test4a_log" "[chown][workdir:/tmp/workdir]" "$CLEANUP_LOG"

echo "All ViashCleanupRegistry tests passed!"

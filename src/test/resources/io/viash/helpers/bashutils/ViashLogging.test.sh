#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashLogging.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh

## TEST1: test log level constants

assert_value_equal "VIASH_LOGCODE_EMERGENCY" "0" "$VIASH_LOGCODE_EMERGENCY"
assert_value_equal "VIASH_LOGCODE_ALERT" "1" "$VIASH_LOGCODE_ALERT"
assert_value_equal "VIASH_LOGCODE_CRITICAL" "2" "$VIASH_LOGCODE_CRITICAL"
assert_value_equal "VIASH_LOGCODE_ERROR" "3" "$VIASH_LOGCODE_ERROR"
assert_value_equal "VIASH_LOGCODE_WARNING" "4" "$VIASH_LOGCODE_WARNING"
assert_value_equal "VIASH_LOGCODE_NOTICE" "5" "$VIASH_LOGCODE_NOTICE"
assert_value_equal "VIASH_LOGCODE_INFO" "6" "$VIASH_LOGCODE_INFO"
assert_value_equal "VIASH_LOGCODE_DEBUG" "7" "$VIASH_LOGCODE_DEBUG"


## TEST2: test default verbosity level

assert_value_equal "VIASH_VERBOSITY" "$VIASH_LOGCODE_NOTICE" "$VIASH_VERBOSITY"


## TEST3: test ViashLog outputs at correct verbosity levels

# TEST3a: Log at notice level (should output at default verbosity)
VIASH_VERBOSITY=$VIASH_LOGCODE_NOTICE
output=$(ViashLog $VIASH_LOGCODE_NOTICE notice "Test message" 2>&1)
assert_value_equal "test3a_output" "[notice] Test message" "$output"

# TEST3b: Log at info level (should NOT output at notice verbosity)
VIASH_VERBOSITY=$VIASH_LOGCODE_NOTICE
output=$(ViashLog $VIASH_LOGCODE_INFO info "Test message" 2>&1)
assert_value_equal "test3b_output" "" "$output"

# TEST3c: Log at info level (should output at info verbosity)
VIASH_VERBOSITY=$VIASH_LOGCODE_INFO
output=$(ViashLog $VIASH_LOGCODE_INFO info "Test message" 2>&1)
assert_value_equal "test3c_output" "[info] Test message" "$output"

# TEST3d: Log at debug level (should NOT output at info verbosity)
VIASH_VERBOSITY=$VIASH_LOGCODE_INFO
output=$(ViashLog $VIASH_LOGCODE_DEBUG debug "Test message" 2>&1)
assert_value_equal "test3d_output" "" "$output"

# TEST3e: Log at error level (should output at notice verbosity)
VIASH_VERBOSITY=$VIASH_LOGCODE_NOTICE
output=$(ViashLog $VIASH_LOGCODE_ERROR error "Test message" 2>&1)
assert_value_equal "test3e_output" "[error] Test message" "$output"


## TEST4: test convenience functions

# Reset to default verbosity
VIASH_VERBOSITY=$VIASH_LOGCODE_DEBUG

# TEST4a: ViashEmergency
output=$(ViashEmergency "Emergency test" 2>&1)
assert_value_equal "test4a_output" "[emergency] Emergency test" "$output"

# TEST4b: ViashAlert
output=$(ViashAlert "Alert test" 2>&1)
assert_value_equal "test4b_output" "[alert] Alert test" "$output"

# TEST4c: ViashCritical
output=$(ViashCritical "Critical test" 2>&1)
assert_value_equal "test4c_output" "[critical] Critical test" "$output"

# TEST4d: ViashError
output=$(ViashError "Error test" 2>&1)
assert_value_equal "test4d_output" "[error] Error test" "$output"

# TEST4e: ViashWarning
output=$(ViashWarning "Warning test" 2>&1)
assert_value_equal "test4e_output" "[warning] Warning test" "$output"

# TEST4f: ViashNotice
output=$(ViashNotice "Notice test" 2>&1)
assert_value_equal "test4f_output" "[notice] Notice test" "$output"

# TEST4g: ViashInfo
output=$(ViashInfo "Info test" 2>&1)
assert_value_equal "test4g_output" "[info] Info test" "$output"

# TEST4h: ViashDebug
output=$(ViashDebug "Debug test" 2>&1)
assert_value_equal "test4h_output" "[debug] Debug test" "$output"


## TEST5: test multiple word messages

VIASH_VERBOSITY=$VIASH_LOGCODE_DEBUG

# TEST5a: Multiple words in message
output=$(ViashInfo "This is a multi-word message" 2>&1)
assert_value_equal "test5a_output" "[info] This is a multi-word message" "$output"

# TEST5b: Message with special characters
output=$(ViashInfo 'Message with $pecial chars: "quotes" and '\''singles'\''' 2>&1)
assert_value_equal "test5b_output" "[info] Message with \$pecial chars: \"quotes\" and 'singles'" "$output"


## TEST6: test verbosity boundary conditions

# TEST6a: Verbosity at emergency (0) should only show emergency
VIASH_VERBOSITY=$VIASH_LOGCODE_EMERGENCY
output=$(ViashEmergency "Emergency" 2>&1)
assert_value_equal "test6a_emergency" "[emergency] Emergency" "$output"
output=$(ViashAlert "Alert" 2>&1)
assert_value_equal "test6a_alert" "" "$output"

# TEST6b: Verbosity at max (debug=7) should show everything
VIASH_VERBOSITY=$VIASH_LOGCODE_DEBUG
output=$(ViashEmergency "Emergency" 2>&1)
assert_value_equal "test6b_emergency" "[emergency] Emergency" "$output"
output=$(ViashDebug "Debug" 2>&1)
assert_value_equal "test6b_debug" "[debug] Debug" "$output"

print_test_summary

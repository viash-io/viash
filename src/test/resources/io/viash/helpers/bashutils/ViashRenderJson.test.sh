#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashRenderJson.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh


## TEST1: test ViashRenderJsonKeyValue with string types

# TEST1a: Single string value
output=$(ViashRenderJsonKeyValue "input" "string" "false" "file.txt")
assert_value_equal "test1a_output" '    "input": "file.txt"' "$output"

# TEST1b: Single string with spaces
output=$(ViashRenderJsonKeyValue "message" "string" "false" "hello world")
assert_value_equal "test1b_output" '    "message": "hello world"' "$output"

# TEST1c: Multiple string values (array)
output=$(ViashRenderJsonKeyValue "inputs" "string" "true" "file1.txt" "file2.txt" "file3.txt")
assert_value_equal "test1c_output" '    "inputs": [ "file1.txt", "file2.txt", "file3.txt" ]' "$output"


## TEST2: test ViashRenderJsonKeyValue with numeric types

# TEST2a: Integer value
output=$(ViashRenderJsonKeyValue "count" "integer" "false" "42")
assert_value_equal "test2a_output" '    "count": 42' "$output"

# TEST2b: Double value
output=$(ViashRenderJsonKeyValue "ratio" "double" "false" "3.14159")
assert_value_equal "test2b_output" '    "ratio": 3.14159' "$output"

# TEST2c: Multiple integer values
output=$(ViashRenderJsonKeyValue "numbers" "integer" "true" "1" "2" "3")
assert_value_equal "test2c_output" '    "numbers": [ 1, 2, 3 ]' "$output"


## TEST3: test ViashRenderJsonKeyValue with boolean types

# TEST3a: Boolean true
output=$(ViashRenderJsonKeyValue "enabled" "boolean" "false" "true")
assert_value_equal "test3a_output" '    "enabled": true' "$output"

# TEST3b: Boolean false
output=$(ViashRenderJsonKeyValue "enabled" "boolean" "false" "false")
assert_value_equal "test3b_output" '    "enabled": false' "$output"

# TEST3c: Boolean yes (should convert to true)
output=$(ViashRenderJsonKeyValue "enabled" "boolean" "false" "yes")
assert_value_equal "test3c_output" '    "enabled": true' "$output"

# TEST3d: Boolean no (should convert to false)
output=$(ViashRenderJsonKeyValue "enabled" "boolean" "false" "no")
assert_value_equal "test3d_output" '    "enabled": false' "$output"

# TEST3e: Boolean TRUE (uppercase, should convert to true)
output=$(ViashRenderJsonKeyValue "enabled" "boolean" "false" "TRUE")
assert_value_equal "test3e_output" '    "enabled": true' "$output"

# TEST3f: Multiple boolean values
output=$(ViashRenderJsonKeyValue "flags" "boolean" "true" "true" "false" "true")
assert_value_equal "test3f_output" '    "flags": [ true, false, true ]' "$output"


## TEST4: test ViashRenderJsonKeyValue with null values

# TEST4a: Single undefined value (should render as null)
output=$(ViashRenderJsonKeyValue "optional" "string" "false" "@@VIASH_UNDEFINED@@")
assert_value_equal "test4a_output" '    "optional": null' "$output"

# TEST4b: Undefined item in array (should render as null in array)
output=$(ViashRenderJsonKeyValue "items" "string" "true" "a" "@@VIASH_UNDEFINED_ITEM@@" "c")
assert_value_equal "test4b_output" '    "items": [ "a", null, "c" ]' "$output"


## TEST5: test ViashRenderJsonKeyValue with file type

# TEST5a: File type (should be quoted like string)
output=$(ViashRenderJsonKeyValue "input" "file" "false" "/path/to/file.txt")
assert_value_equal "test5a_output" '    "input": "/path/to/file.txt"' "$output"

# TEST5b: Multiple file values
output=$(ViashRenderJsonKeyValue "inputs" "file" "true" "/a.txt" "/b.txt")
assert_value_equal "test5b_output" '    "inputs": [ "/a.txt", "/b.txt" ]' "$output"


## TEST6: test ViashRenderJsonQuotedValue with special characters

# TEST6a: String with quotes (should be escaped)
output=$(ViashRenderJsonQuotedValue "key" 'say "hello"')
assert_value_equal "test6a_output" '"say \"hello\""' "$output"

# TEST6b: String with backslash (should be escaped)
output=$(ViashRenderJsonQuotedValue "key" 'path\to\file')
assert_value_equal "test6b_output" '"path\\to\\file"' "$output"

# TEST6c: String with newline (should be escaped)
output=$(ViashRenderJsonQuotedValue "key" $'line1\nline2')
assert_value_equal "test6c_output" '"line1\nline2"' "$output"

# TEST6d: String with all special characters
output=$(ViashRenderJsonQuotedValue "key" 'a"b\c')
assert_value_equal "test6d_output" '"a\"b\\c"' "$output"

# TEST6e: String with backticks (should be preserved as literal)
output=$(ViashRenderJsonQuotedValue "key" 'echo `whoami`')
assert_value_equal "test6e_backticks" '"echo `whoami`"' "$output"

# TEST6f: String with dollar sign (should be preserved as literal)
output=$(ViashRenderJsonQuotedValue "key" 'value is $HOME')
assert_value_equal "test6f_dollar" '"value is $HOME"' "$output"

# TEST6g: String with command substitution syntax (should be preserved as literal)
output=$(ViashRenderJsonQuotedValue "key" 'run $(date)')
assert_value_equal "test6g_subst" '"run $(date)"' "$output"

# TEST6h: String with single quotes
output=$(ViashRenderJsonQuotedValue "key" "it's a test")
assert_value_equal "test6h_singlequote" '"it'\''s a test"' "$output"

# TEST6i: String with tab character
output=$(ViashRenderJsonQuotedValue "key" $'col1\tcol2')
assert_value_equal "test6i_tab" '"col1\tcol2"' "$output"

# TEST6j: String with carriage return
output=$(ViashRenderJsonQuotedValue "key" $'line1\rline2')
assert_value_equal "test6j_cr" '"line1\rline2"' "$output"

# TEST6k: Complex string with multiple special chars
output=$(ViashRenderJsonQuotedValue "key" 'a\b"c$d`e')
assert_value_equal "test6k_complex" '"a\\b\"c$d`e"' "$output"


## TEST7: test ViashRenderJsonBooleanValue

# TEST7a: true
output=$(ViashRenderJsonBooleanValue "flag" "true")
assert_value_equal "test7a_output" "true" "$output"

# TEST7b: false
output=$(ViashRenderJsonBooleanValue "flag" "false")
assert_value_equal "test7b_output" "false" "$output"

# TEST7c: yes
output=$(ViashRenderJsonBooleanValue "flag" "yes")
assert_value_equal "test7c_output" "true" "$output"

# TEST7d: no
output=$(ViashRenderJsonBooleanValue "flag" "no")
assert_value_equal "test7d_output" "false" "$output"

# TEST7e: YES (uppercase)
output=$(ViashRenderJsonBooleanValue "flag" "YES")
assert_value_equal "test7e_output" "true" "$output"


## TEST8: test ViashRenderJsonUnquotedValue

# TEST8a: Simple value
output=$(ViashRenderJsonUnquotedValue "num" "42")
assert_value_equal "test8a_output" "42" "$output"

# TEST8b: Decimal value
output=$(ViashRenderJsonUnquotedValue "num" "3.14")
assert_value_equal "test8b_output" "3.14" "$output"


## TEST9: test single value array

# TEST9a: Single element in multiple mode
output=$(ViashRenderJsonKeyValue "items" "string" "true" "only_one")
assert_value_equal "test9a_output" '    "items": [ "only_one" ]' "$output"

# TEST9b: Empty string value
output=$(ViashRenderJsonKeyValue "empty" "string" "false" "")
assert_value_equal "test9b_output" '    "empty": ""' "$output"


## TEST10: test ViashRenderJsonKeyValue with dangerous shell characters

# TEST10a: String value with backticks
output=$(ViashRenderJsonKeyValue "cmd" "string" "false" 'run `id`')
assert_value_equal "test10a_backtick" '    "cmd": "run `id`"' "$output"

# TEST10b: String value with dollar sign
output=$(ViashRenderJsonKeyValue "env" "string" "false" 'path is $PATH')
assert_value_equal "test10b_dollar" '    "env": "path is $PATH"' "$output"

# TEST10c: String value with command substitution
output=$(ViashRenderJsonKeyValue "sub" "string" "false" 'time $(date)')
assert_value_equal "test10c_subst" '    "sub": "time $(date)"' "$output"

# TEST10d: String value with backslash
output=$(ViashRenderJsonKeyValue "path" "string" "false" 'C:\Users\test')
assert_value_equal "test10d_backslash" '    "path": "C:\\Users\\test"' "$output"

# TEST10e: Array with dangerous characters
output=$(ViashRenderJsonKeyValue "items" "string" "true" 'a`b' 'c$d' 'e\f')
assert_value_equal "test10e_array" '    "items": [ "a`b", "c$d", "e\\f" ]' "$output"

# TEST10f: Multiple backslashes
output=$(ViashRenderJsonKeyValue "path" "string" "false" 'a\\b\\\\c')
assert_value_equal "test10f_multibackslash" '    "path": "a\\\\b\\\\\\\\c"' "$output"

# TEST10g: Nested quotes
output=$(ViashRenderJsonKeyValue "nested" "string" "false" 'He said "Hello"')
assert_value_equal "test10g_nestedquotes" '    "nested": "He said \"Hello\""' "$output"

# TEST10h: Unicode and special chars (if supported)
output=$(ViashRenderJsonKeyValue "unicode" "string" "false" 'café')
assert_value_equal "test10h_unicode" '    "unicode": "café"' "$output"

# INTENTIONAL FAILURE: Remove this test after verifying test framework catches failures
output=$(ViashRenderJsonKeyValue "test" "string" "false" 'hello')
assert_value_equal "INTENTIONAL_FAILURE" '    "test": "WRONG_VALUE"' "$output"

print_test_summary

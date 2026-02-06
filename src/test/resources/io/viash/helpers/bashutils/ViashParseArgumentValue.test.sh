#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashParseArgumentValue.sh
source src/main/resources/io/viash/helpers/bashutils/ViashLogging.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh

## TEST1: test simple strings

# TEST1a simple use case
ViashParseArgumentValue "--input" "par_input" "false" 'input.txt'

assert_value_equal "par_input" 'input.txt' "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input

# TEST1b multiple values
ViashParseArgumentValue "--input" "par_input" "true" 'input1.txt;input2.txt'

assert_value_equal "par_input" 'input1.txt input2.txt' "${par_input[@]}"
assert_value_equal "par_input" 2 "${#par_input[@]}"

unset par_input




## TEST2: test quoting

# TEST2a resolve quotes
ViashParseArgumentValue "--input" "par_input" "false" '"input.txt"'

assert_value_equal "par_input" 'input.txt' "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input

# TEST2b resolve single quotes

ViashParseArgumentValue "--input" "par_input" "false" "'input.txt'"

assert_value_equal "par_input" 'input.txt' "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input

# TEST2c resolve quotes with multiple values
ViashParseArgumentValue "--input" "par_input" "true" '"input1.txt";"input2.txt"'

assert_value_equal "par_input" 'input1.txt input2.txt' "${par_input[@]}"
assert_value_equal "par_input" 2 "${#par_input[@]}"

unset par_input

# TEST2d resolve quotes with quotes in the value
ViashParseArgumentValue "--input" "par_input" "false" "\"foo'bar'\""

# todo: check
assert_value_equal "par_input" "foo'bar'" "${par_input[@]}"
unset par_input



## TEST3: test undefined

# TEST3a escape undefined
ViashParseArgumentValue "--input" "par_input" "false" 'UNDEFINED'

assert_value_equal "par_input" '@@VIASH_UNDEFINED@@' "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input

# TEST3b do not escape undefined when quoted
ViashParseArgumentValue "--input" "par_input" "false" '"UNDEFINED"' "false"

assert_value_equal "par_input" 'UNDEFINED' "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input

# TEST3c do not escape undefined when single quoted
ViashParseArgumentValue "--input" "par_input" "false" "'UNDEFINED'" "false"

assert_value_equal "par_input" 'UNDEFINED' "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input

# TEST3d escape undefined when multiple values
ViashParseArgumentValue "--input" "par_input" "true" 'UNDEFINED'

assert_value_equal "par_input" '@@VIASH_UNDEFINED@@' "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input

# TEST3e do not escape undefined when multiple values and quoted
ViashParseArgumentValue "--input" "par_input" "true" '"UNDEFINED"' "false"

assert_value_equal "par_input" 'UNDEFINED' "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input

# TEST3f do not escape undefined when multiple values and single quoted
ViashParseArgumentValue "--input" "par_input" "true" "'UNDEFINED'" "false"

assert_value_equal "par_input" 'UNDEFINED' "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input


## TEST4: test undefined_item

# TEST4a do not escape undefined_item for single value
ViashParseArgumentValue "--input" "par_input" "false" 'UNDEFINED_ITEM'

assert_value_equal "par_input" 'UNDEFINED_ITEM' "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input

# TEST4b escape undefined_item for multiple values
ViashParseArgumentValue "--input" "par_input" "true" 'UNDEFINED_ITEM;a;b;UNDEFINED_ITEM'

assert_value_equal "par_input" '@@VIASH_UNDEFINED_ITEM@@ a b @@VIASH_UNDEFINED_ITEM@@' "${par_input[@]}"
assert_value_equal "par_input" 4 "${#par_input[@]}"

unset par_input

# TEST4c do not escape undefined_item for multiple values when quoted
ViashParseArgumentValue "--input" "par_input" "true" "\"UNDEFINED_ITEM\";a;'UNDEFINED_ITEM';UNDEFINED_ITEM"

assert_value_equal "par_input" 'UNDEFINED_ITEM a UNDEFINED_ITEM @@VIASH_UNDEFINED_ITEM@@' "${par_input[@]}"
assert_value_equal "par_input" 4 "${#par_input[@]}"

unset par_input


## TEST5: test escaping of special characters

# TEST5a do not escape single strings
ViashParseArgumentValue "--input" "par_input" "false" "a\;b\'\\\"c"

assert_value_equal "par_input" "a\;b\'\\\"c" "${par_input[@]}"
assert_value_equal "par_input" 1 "${#par_input[@]}"

unset par_input

# TEST5b escape multiple values
ViashParseArgumentValue "--input" "par_input" "true" "a\;b\'\\\"c;d;e"

assert_value_equal "par_input" "a;b'\"c d e" "${par_input[@]}"
assert_value_equal "par_input" 3 "${#par_input[@]}"

unset par_input

# TEST5c escape multiple values with quotes
ViashParseArgumentValue "--input" "par_input" "true" "\"a\;b\'\\\"c\";d;'e'"
assert_value_equal "par_input" "a;b'\"c d e" "${par_input[@]}"
assert_value_equal "par_input" 3 "${#par_input[@]}"

unset par_input

# TEST5d escape multiple values with single quotes
ViashParseArgumentValue "--input" "par_input" "true" "'a\;b\'\\\"c';d;'e'"

assert_value_equal "par_input" "a;b'\"c d e" "${par_input[@]}"
assert_value_equal "par_input" 3 "${#par_input[@]}"

unset par_input


## TEST6: test special shell characters that could cause command injection

# TEST6a: backticks should not be executed
ViashParseArgumentValue "--input" "par_input" "false" 'value with `echo dangerous`'

assert_value_equal "par_input_backtick" 'value with `echo dangerous`' "${par_input[@]}"
assert_value_equal "par_input_backtick_len" 1 "${#par_input[@]}"

unset par_input

# TEST6b: dollar signs should be preserved literally
ViashParseArgumentValue "--input" "par_input" "false" 'value with $HOME variable'

assert_value_equal "par_input_dollar" 'value with $HOME variable' "${par_input[@]}"
assert_value_equal "par_input_dollar_len" 1 "${#par_input[@]}"

unset par_input

# TEST6c: command substitution syntax should not be executed
ViashParseArgumentValue "--input" "par_input" "false" 'value with $(whoami)'

assert_value_equal "par_input_subst" 'value with $(whoami)' "${par_input[@]}"
assert_value_equal "par_input_subst_len" 1 "${#par_input[@]}"

unset par_input

# TEST6d: complex string with multiple special characters
ViashParseArgumentValue "--input" "par_input" "false" 'a \ b $ c ` d " e '\'' f'

assert_value_equal "par_input_complex" 'a \ b $ c ` d " e '\'' f' "${par_input[@]}"
assert_value_equal "par_input_complex_len" 1 "${#par_input[@]}"

unset par_input

# TEST6e: newline escape sequences
ViashParseArgumentValue "--input" "par_input" "false" 'line1\nline2'

assert_value_equal "par_input_newline" 'line1\nline2' "${par_input[@]}"
assert_value_equal "par_input_newline_len" 1 "${#par_input[@]}"

unset par_input


## TEST7: test accumulating multiple values

# TEST7a: multiple calls should accumulate values
ViashParseArgumentValue "--input" "par_input" "true" 'value1'
ViashParseArgumentValue "--input" "par_input" "true" 'value2'

assert_value_equal "par_input_accum" 'value1 value2' "${par_input[@]}"
assert_value_equal "par_input_accum_len" 2 "${#par_input[@]}"

unset par_input

# TEST7b: multiple calls with semicolon-separated values
ViashParseArgumentValue "--input" "par_input" "true" 'a;b'
ViashParseArgumentValue "--input" "par_input" "true" 'c;d'

assert_value_equal "par_input_multi_accum" 'a b c d' "${par_input[@]}"
assert_value_equal "par_input_multi_accum_len" 4 "${#par_input[@]}"

unset par_input


## TEST8: test empty and whitespace values

# TEST8a: empty string for multiple
ViashParseArgumentValue "--input" "par_input" "true" ''

assert_value_equal "par_input_empty_multi" '' "${par_input[@]}"
assert_value_equal "par_input_empty_multi_len" 1 "${#par_input[@]}"

unset par_input

# TEST8b: value with only spaces
ViashParseArgumentValue "--input" "par_input" "false" '   '

assert_value_equal "par_input_spaces" '   ' "${par_input[@]}"
assert_value_equal "par_input_spaces_len" 1 "${#par_input[@]}"

unset par_input


echo "All ViashParseArgumentValue tests passed!"
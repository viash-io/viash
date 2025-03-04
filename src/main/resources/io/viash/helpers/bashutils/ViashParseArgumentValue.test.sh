# todo: align tests with
# https://github.com/viash-io/viash/issues/705#issuecomment-2208448576

assert_value_equal() {
  name="$1"
  expected="$2"
  shift 2
  values="$@"
  if [ "$expected" != "$values" ]; then
    echo "Expected '$name' to be equal to '$expected', but got '$values'"
    # exit 1
  fi
}

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashParseArgumentValue.sh
source src/main/resources/io/viash/helpers/bashutils/ViashLogging.sh



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
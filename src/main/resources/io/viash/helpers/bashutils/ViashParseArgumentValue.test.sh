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

unset par_input

# TEST2d resolve quotes with quotes in the value
ViashParseArgumentValue "--input" "par_input" "false" '"foo'bar'"'






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


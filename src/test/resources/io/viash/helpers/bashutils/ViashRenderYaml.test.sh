#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashLogging.sh
source src/main/resources/io/viash/helpers/bashutils/ViashRenderYaml.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh


## TEST1: test simple strings

# TEST1a simple use case
out_1a=$(ViashRenderYamlKeyValue input file false input.txt)
assert_value_equal "out_1a" '  input: "input.txt"' "$out_1a"

# TEST1b multiple values
out_1b=$(ViashRenderYamlKeyValue input file true input1.txt input2.txt)
assert_value_equal "out_1b" '  input:
    - "input1.txt"
    - "input2.txt"' "$out_1b"


# TEST2: test quoting

# TEST2a resolve quotes
out_2a=$(ViashRenderYamlKeyValue input file false '"input.txt"')
assert_value_equal "out_2a" '  input: "\"input.txt\""' "$out_2a"

# TEST2b resolve single quotes
out_2b=$(ViashRenderYamlKeyValue input file false "'input.txt'")
assert_value_equal "out_2b" '  input: "'"'"'input.txt'"'"'"' "$out_2b"

# TEST2c resolve quotes with multiple values
out_2c=$(ViashRenderYamlKeyValue input file true '"input1.txt"' "'input2.txt'")
assert_value_equal "out_2c" '  input:
    - "\"input1.txt\""
    - "'"'"'input2.txt'"'"'"' "$out_2c"

# TEST2d resolve quotes with quotes in the value
out_2d=$(ViashRenderYamlKeyValue input file false "\"foo'bar'\"")
assert_value_equal "out_2d" '  input: "\"foo'"'"'bar'"'"'\""' "$out_2d"



## TEST3: test undefined

# TEST3a resolve undefined
out_3a=$(ViashRenderYamlKeyValue input file false @@VIASH_UNDEFINED@@)
assert_value_equal "out_3a" '  input: null' "$out_3a"

# TEST3b resolve undefined with multiple values
out_3b=$(ViashRenderYamlKeyValue input file true @@VIASH_UNDEFINED_ITEM@@ input1.txt input2.txt)
assert_value_equal "out_3b" '  input:
    - null
    - "input1.txt"
    - "input2.txt"' "$out_3b"

# TEST3c do not resolve undefined when quoted
out_3c=$(ViashRenderYamlKeyValue input file false '"UNDEFINED"')
assert_value_equal "out_3c" '  input: "\"UNDEFINED\""' "$out_3c"


## TEST4: test escape special characters

# TEST4a escape backslashes
out_4a=$(ViashRenderYamlKeyValue input file false 'foo\\bar')
assert_value_equal "out_4a" '  input: "foo\\\\bar"' "$out_4a"

# TEST4b escape quotes
out_4b=$(ViashRenderYamlKeyValue input file false 'foo"bar')
assert_value_equal "out_4b" '  input: "foo\"bar"' "$out_4b"

# TEST4c escape newlines
value_with_newline="foo
bar"
out_4c=$(ViashRenderYamlKeyValue input file false "$value_with_newline")
assert_value_equal "out_4c" '  input: "foo\nbar"' "$out_4c"


## TEST5: test boolean values

# TEST5a resolve single boolean value
out_5a=$(ViashRenderYamlKeyValue input boolean false true)
assert_value_equal "out_5a" '  input: true' "$out_5a"

# TEST5b resolve multiple boolean values
out_5b=$(ViashRenderYamlKeyValue input boolean true true false true)
assert_value_equal "out_5b" '  input:
    - true
    - false
    - true' "$out_5b"


## TEST6: test unquoted values

# TEST6a resolve integer
out_6a=$(ViashRenderYamlKeyValue input integer false 42)
assert_value_equal "out_6a" '  input: 42' "$out_6a"

# TEST6b resolve float
out_6b=$(ViashRenderYamlKeyValue input float false 3.14)
assert_value_equal "out_6b" '  input: 3.14' "$out_6b"

# TEST6c resolve multiple integers
out_6c=$(ViashRenderYamlKeyValue input integer true 42 43 44)
assert_value_equal "out_6c" '  input:
    - 42
    - 43
    - 44' "$out_6c"

# TEST6d resolve multiple floats
out_6d=$(ViashRenderYamlKeyValue input float true 3.14 2.71 1.41)
assert_value_equal "out_6d" '  input:
    - 3.14
    - 2.71
    - 1.41' "$out_6d"

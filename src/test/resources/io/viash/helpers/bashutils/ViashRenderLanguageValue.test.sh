#!/bin/bash

# load helper functions
source src/main/resources/io/viash/helpers/bashutils/ViashLogging.sh
source src/main/resources/io/viash/helpers/bashutils/ViashRenderLanguageValue.sh
source src/test/resources/io/viash/helpers/bashutils/helpers.sh


## TEST1: test simple strings

# TEST1a simple use case
out_1a=$(ViashRenderPythonValue file false input.txt)
assert_value_equal "out_1a" "r'input.txt'" "$out_1a"

# TEST1b multiple values
out_1b=$(ViashRenderPythonValue file true input1.txt input2.txt)
assert_value_equal "out_1b" "[r'input1.txt', r'input2.txt']" "$out_1b"


# TEST2: test quoting

# TEST2a resolve quotes
out_2a=$(ViashRenderPythonValue file false '"input.txt"')
assert_value_equal "out_2a" "r'\"input.txt\"'" "$out_2a"

# TEST2b resolve single quotes
out_2b=$(ViashRenderPythonValue file false "'input.txt'")
assert_value_equal "out_2b" "r'\\'input.txt\\''" "$out_2b"

# TEST2c resolve quotes with multiple values
out_2c=$(ViashRenderPythonValue file true '"input1.txt"' "'input2.txt'")
assert_value_equal "out_2c" "[r'\"input1.txt\"', r'\\'input2.txt\\'']" "$out_2c"

# TEST2d resolve quotes with quotes in the value
out_2d=$(ViashRenderPythonValue file false "\"foo'bar'\"")
assert_value_equal "out_2d" "r'\"foo\\'bar\\'\"'" "$out_2d"



## TEST3: test undefined

# TEST3a resolve undefined
out_3a=$(ViashRenderPythonValue file false @@VIASH_UNDEFINED@@)
assert_value_equal "out_3a" "None" "$out_3a"

# TEST3b resolve undefined with multiple values
out_3b=$(ViashRenderPythonValue file true @@VIASH_UNDEFINED_ITEM@@ input1.txt input2.txt)
assert_value_equal "out_3b" "[None, r'input1.txt', r'input2.txt']" "$out_3b"

# TEST3c do not resolve undefined when quoted
out_3c=$(ViashRenderPythonValue file false '"UNDEFINED"')
assert_value_equal "out_3c" "r'\"UNDEFINED\"'" "$out_3c"


## TEST4: test escape special characters

# TEST4a escape backslashes
out_4a=$(ViashRenderPythonValue file false 'foo\\bar')
assert_value_equal "out_4a" "r'foo\\\\bar'" "$out_4a"

# TEST4b escape quotes
out_4b=$(ViashRenderPythonValue file false 'foo"bar')
assert_value_equal "out_4b" '  input: "foo\"bar"' "$out_4b"

# TEST4c escape newlines
value_with_newline="foo
bar"
out_4c=$(ViashRenderPythonValue file false "$value_with_newline")
assert_value_equal "out_4c" '  input: "foo\nbar"' "$out_4c"

exit 1 # todo: contrinue with the rest of the tests
# ## TEST5: test boolean values

# # TEST5a resolve single boolean value
# out_5a=$(ViashRenderPythonValue boolean false true)
# assert_value_equal "out_5a" '  input: true' "$out_5a"

# # TEST5b resolve multiple boolean values
# out_5b=$(ViashRenderPythonValue boolean true true false true)
# assert_value_equal "out_5b" '  input:
#     - true
#     - false
#     - true' "$out_5b"


# ## TEST6: test unquoted values

# # TEST6a resolve integer
# out_6a=$(ViashRenderPythonValue integer false 42)
# assert_value_equal "out_6a" '  input: 42' "$out_6a"

# # TEST6b resolve float
# out_6b=$(ViashRenderPythonValue float false 3.14)
# assert_value_equal "out_6b" '  input: 3.14' "$out_6b"

# # TEST6c resolve multiple integers
# out_6c=$(ViashRenderPythonValue integer true 42 43 44)
# assert_value_equal "out_6c" '  input:
#     - 42
#     - 43
#     - 44' "$out_6c"

# # TEST6d resolve multiple floats
# out_6d=$(ViashRenderPythonValue float true 3.14 2.71 1.41)
# assert_value_equal "out_6d" '  input:
#     - 3.14
#     - 2.71
#     - 1.41' "$out_6d"

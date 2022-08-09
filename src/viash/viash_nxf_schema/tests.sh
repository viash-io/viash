#!/usr/bin/env bash

# set -ex

cat > config_with_groups.yaml << EOF
functionality:
  name: test_workflow
  # description: Check if empty fields do not raise an exception
  arguments:
    - name: --filearg
      type: file
      required: true
      default: file.txt
      description: Description for filearg.
    - name: --stringarg
      type: string
      default: string_value
      description: Description for stringarg.
    - name: --numberarg
      type: integer
      default: 1
      description: Description for numberarg
    - name: --doublearg
      type: double
      default: 1.0
      description: Description for doublearg
  argument_groups:
    - name: Files
      description: All arguments related to files.
      arguments: [ filearg ]
    - name: Numbers
      description: Arguments related to numbers.
      arguments: [ numberarg, doublearg ]
    - name: Strings
      description: Arguments related to strings.
      arguments: [ stringarg ]
EOF

echo "Run the component with groups defined"
./$meta_functionality_name \
    --config config_with_groups.yaml \
    --schema schema_with_groups.json

echo "Check if output is written"
[[ ! -f schema_with_groups.json ]] && echo "No schema written" && exit 1

echo "Lint the schema file"
nf-core schema lint schema_with_groups.json 2> with_groups.lint
grep -q Error with_groups.lint
[[ $? == 0 ]] && echo -e "Schema linting failed\nFull output of the linting:" && cat with_groups.lint && exit 1


##############################################

cat > config.yaml << EOF
functionality:
  name: test_workflow
  # description: Check if empty fields do not raise an exception
  arguments:
    - name: --filearg
      type: file
      required: true
      default: file.txt
      description: Description for filearg.
    - name: --stringarg
      type: string
      default: string_value
      description: Description for stringarg.
    - name: --numberarg
      type: integer
      default: 1
      description: Description for numberarg
    - name: --doublearg
      type: double
      default: 1.0
      description: Description for doublearg
  argument_groups:
    - name: Files
      description: All arguments related to files.
      arguments: [ filearg ]
    - name: Numbers
      description: Arguments related to numbers.
      arguments: [ numberarg, doublearg ]
    - name: Strings
      description: Arguments related to strings.
      arguments: [ stringarg ]
EOF

echo "Run the component without groups defined"
./$meta_functionality_name \
    --config config.yaml \
    --schema schema.json

echo "Check if output is written"
[[ ! -f schema.json ]] && echo "No schema written" && exit 1

echo "Lint the schema file"
nf-core schema lint schema.json 2> no_groups.lint
grep -q Error no_groups.lint
[[ $? == 0 ]] && echo -e "Schema linting failed\nFull output of the linting:" && cat no_groups.lint && exit 1

echo "Test ok"

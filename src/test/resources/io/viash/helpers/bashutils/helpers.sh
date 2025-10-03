#!/bin/bash

assert_value_equal() {
  name="$1"
  expected="$2"
  shift 2
  values="$@"
  if [ "$expected" != "$values" ]; then
    echo "Expected '$name' to be equal to '$expected', but got '$values'"
  fi
}

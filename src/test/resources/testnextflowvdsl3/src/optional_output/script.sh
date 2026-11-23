#!/bin/bash

if [ -z "$par_optional_output" ]; then
  echo "optional_output: unset" > "$par_output"
else
  echo "optional_output: set" > "$par_output"
  echo "optional content" > "$par_optional_output"
fi

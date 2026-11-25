#!/bin/bash

if [ -z "$par_optional_outputs" ]; then
  echo "optional_outputs: unset" > "$par_output"
else
  echo "optional_outputs: set" > "$par_output"
  for i in 0 1; do
    echo "optional content $i" > "${par_optional_outputs//\*/$i}"
  done
fi

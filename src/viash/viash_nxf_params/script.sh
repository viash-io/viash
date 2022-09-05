#!/bin/bash

nextflow -q run \
  "$resources_dir/main.nf" \
  --resourcesDir "$resources_dir" \
  --input "$par_input" \
  --addGlobals "$par_add_globals" > out.txt

if [ -z "$par_output" ]; then
  cat out.txt
else
  cat out.txt > "$par_output"
fi
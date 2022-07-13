#!/bin/bash

nextflow -q run \
  "$resources_dir/main.nf" \
  --resourcesDir "$resources_dir" \
  --input "$par_input" \
  --output config.json

jinja -d config.json "$meta_resources_dir/template.j2" > "$par_output"

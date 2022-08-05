#!/bin/bash

nextflow -q run \
  "$resources_dir/main.nf" \
  --resourcesDir "$resources_dir" \
  --input "$par_input" \
  --output config.json

# workaround for the additional / in the group name
sed -i 's|input/output|input-output|' config.json

jinja -d config.json "$meta_resources_dir/template.j2" > "$par_output"

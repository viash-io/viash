#!/bin/bash

### VIASH START
par_input="target/docker"
### VIASH END

# find all viash.yaml instances
for i in `find $par_input -name viash.yaml`; do

  tool_src_dir=`dirname $i`
  tool_name=`basename "$tool_src_dir"`

  echo "Processing $tool_name"

  "$tool_src_dir/$tool_name" ---setup

done

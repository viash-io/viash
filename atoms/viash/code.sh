#!/bin/bash

# VIASH START
par_functionality="functionality.yaml"
par_platform="platform.yaml"
par_mode="export"
par_output="/tmp/viash/"
# VIASH END

java -jar $RESOURCES_DIR/viash-assembly-0.1.0.jar \
  $par_mode \
  --functionality $par_functionality \
  --platform $par_platform \
  --output $par_output

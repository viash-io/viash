#!/bin/bash

# PORTASH START
par_functionality="functionality.yaml"
par_platform="platform.yaml"
par_mode="export"
par_output="test/"
PASSTHROUGH="-b" # passthrough
# PORTASH END

java -jar $RESOURCES_DIR/viash-assembly-0.0.1.jar --functionality $par_functionality --platform $par_platform $par_mode --output $par_output

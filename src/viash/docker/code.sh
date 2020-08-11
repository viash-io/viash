#!/bin/bash

# VIASH START
par_mode="export"
par_functionality="viash"
par_platform=""
par_output="target"
# VIASH END

eval viash "$par_mode" -f "$par_functionality" -p "$par_platform" -o "$par_output"

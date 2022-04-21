#!/bin/bash

export NXF_VER=21.04.1

nextflow \
  run . \
  -main-script workflows/pipeline4/main.nf \
  --input "resources/*" \
  --publishDir "output" \
  -resume \
  --displayDebug true

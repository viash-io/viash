#!/bin/bash

export NXF_VER=21.04.1

nextflow \
  run . \
  -main-script workflows/main.nf \
  --input "resources/*" \
  --publishDir "output" \
  -resume

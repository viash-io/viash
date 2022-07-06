#!/bin/bash

export NXF_VER=21.04.1

nextflow \
  -q \
  run . \
  -main-script workflows/pipeline3/main.nf \
  --id foo \
  --input "resources/lines3.txt" \
  --real_number 10.5 \
  --whole_number 10 \
  --str foo \
  --publishDir "output" \
  -resume \
  -entry base

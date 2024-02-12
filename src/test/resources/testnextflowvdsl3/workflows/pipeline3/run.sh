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
  --multiple "a;b;c;d" \
  --publishDir "output" \
  -resume \
  -entry base

fooArgs="{id: foo, input: resources/lines3.txt, whole_number: 3, optional_with_default: foo, multiple: [a, b, c]}"
barArgs="{id: bar, input: resources/lines5.txt, real_number: 0.5, optional: bar}"
nextflow \
  -q \
  run . \
  -main-script workflows/pipeline3/main.nf \
  --param_list "[$fooArgs, $barArgs]" \
  --real_number 10.5 \
  --whole_number 10 \
  --str foo \
  --publishDir "output" \
  -resume \
  -entry base
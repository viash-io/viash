#!/usr/bin/env bash

echo ">>> Create a work directory by running Nextflow"
nextflow run https://github.com/nextflow-io/hello

echo ">>> Running Cleanup"
./project_clean

echo ">>> Checking result"
nextflow log | cut -f 3 | tail -1 | xargs nextflow log | tee output.log
[[ ! `cat output.log` == "" ]] && echo "There is still cache data here" && exit 1

echo ">>> Test ok"

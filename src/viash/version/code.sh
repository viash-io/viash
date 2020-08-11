#!/bin/bash

# VIASH START
par_version="0.1"
par_location="src/viash"
# VIASH END

for i in `find "$par_location" -name functionality.yaml`; do
  echo "Inserting/replacing version in $i"
  yq w -i --tag '!!str' "$i" version "$par_version"
done

for i in `find "$par_location" -name platform*.yaml`; do
  echo "Inserting/replacing version in $i"
  yq w -i --tag '!!str' "$i" version "$par_version"
done

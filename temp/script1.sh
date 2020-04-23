#!/bin/bash

source temp/helper.sh

# run_tests
assembly
# assembly_without_testing

viash_export_all atoms/ output/
# viash_export_all src/test/resources/


# Some custom runs (NXF related)
viash \
      -f atoms/wrapped_filter/functionality.yaml \
      -p atoms/wrapped_filter/platform_native.yaml \
      export \
      -o output/wrapped_filter_nextflow/bin

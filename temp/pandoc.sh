#!/bin/bash

source temp/helper.sh

viash export -f atoms/md_concat/functionality.yaml -p atoms/md_concat/platform_nextflow.yaml -o output/pandoc_pipeline/modules/md_concat
viash export -f atoms/md_add_toc/functionality.yaml -p atoms/md_add_toc/platform_nextflow.yaml -o output/pandoc_pipeline/modules/md_add_toc
viash export -f atoms/md_to_html/functionality.yaml -p atoms/md_to_html/platform_nextflow.yaml -o output/pandoc_pipeline/modules/md_to_html

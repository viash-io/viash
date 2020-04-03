#!/bin/bash

# sbt assembly

JAVA="java -jar target/scala-2.12/viash-assembly-0.0.1.jar"

$JAVA -f atoms/md_concat/functionality.yaml -p atoms/md_concat/platform_nextflow.yaml export -o output/pandoc_pipeline/modules/md_concat
$JAVA -f atoms/md_add_toc/functionality.yaml -p atoms/md_add_toc/platform_nextflow.yaml export -o output/pandoc_pipeline/modules/md_add_toc
$JAVA -f atoms/md_to_html/functionality.yaml -p atoms/md_to_html/platform_nextflow.yaml export -o output/pandoc_pipeline/modules/md_to_html

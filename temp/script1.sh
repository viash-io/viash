#!/bin/bash

sbt assembly

JAVA="java -jar target/scala-2.12/viash-assembly-0.0.1.jar"

$JAVA -f atoms/print_params/functionality.yaml -p atoms/print_params/platform_docker.yaml export -o output/print_params_docker
$JAVA -f atoms/print_params/functionality.yaml -p atoms/print_params/platform_native.yaml export -o output/print_params_native
$JAVA -f atoms/filter/functionality.yaml -p atoms/filter/platform_docker.yaml export -o output/filter_docker
$JAVA -f atoms/filter/functionality.yaml -p atoms/filter/platform_native.yaml export -o output/filter_native
$JAVA -f atoms/pandoc/functionality.yaml -p atoms/pandoc/platform_docker.yaml export -o output/pandoc_docker
$JAVA -f atoms/pandoc/functionality.yaml -p atoms/pandoc/platform_nextflow.yaml export -o output/pandoc
$JAVA -f atoms/testbash/functionality.yaml -p atoms/testbash/platform_native.yaml export -o output/testbash_native

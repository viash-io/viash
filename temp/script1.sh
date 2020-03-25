#!/bin/bash

sbt "run -f atoms/print_params/functionality.yaml -p atoms/print_params/platform_docker.yaml export -o output/print_params_docker"
sbt "run -f atoms/print_params/functionality.yaml -p atoms/print_params/platform_native.yaml export -o output/print_params_native"
sbt "run -f atoms/filter/functionality.yaml -p atoms/filter/platform_docker.yaml export -o output/filter_docker"
sbt "run -f atoms/filter/functionality.yaml -p atoms/filter/platform_native.yaml export -o output/filter_native"

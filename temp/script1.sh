#!/bin/bash

red=`tput setaf 1`
green=`tput setaf 2`
blue=`tput setaf 4`
reset=`tput sgr0`

# sbt assembly

# sbt assembly

platforms=("native" "nextflow" "docker")

JAVA="java -jar target/scala-2.12/viash-assembly-0.0.1.jar"

for a in `ls atoms`; do
  echo ">> ${green}Processing atom $a${reset}"
  func_file="atoms/$a/functionality.yaml"
  for p in ${platforms[@]}; do
    echo ">>>> ${blue}Processing platform $p${reset}"
    platform_file="atoms/$a/platform_$p.yaml"
    if [ -f $platform_file ]; then
      # echo "      $func_file"
      # echo "      $platform_file"
      # echo "$JAVA -f $func_file -p $platform_file export -o output/${a}_${p}"
      $JAVA -f $func_file \
            -p $platform_file \
            export \
            -o "output/${a}_${p}"
    fi
  done
done

# $JAVA -f atoms/print_params/functionality.yaml -p atoms/print_params/platform_docker.yaml export -o output/print_params_docker
# $JAVA -f atoms/print_params/functionality.yaml -p atoms/print_params/platform_native.yaml export -o output/print_params_native
# $JAVA -f atoms/filter/functionality.yaml -p atoms/filter/platform_docker.yaml export -o output/filter_docker
# $JAVA -f atoms/filter/functionality.yaml -p atoms/filter/platform_native.yaml export -o output/filter_native
# $JAVA -f atoms/pandoc/functionality.yaml -p atoms/pandoc/platform_docker.yaml export -o output/pandoc_docker
# $JAVA -f atoms/pandoc/functionality.yaml -p atoms/pandoc/platform_nextflow.yaml export -o output/pandoc
# $JAVA -f atoms/testbash/functionality.yaml -p atoms/testbash/platform_native.yaml export -o output/testbash_native
# $JAVA -f atoms/testbash/functionality.yaml -p atoms/testbash/platform_docker.yaml export -o output/testbash_docker
# $JAVA -f atoms/wrapped_filter/functionality.yaml -p atoms/wrapped_filter/platform_native.yaml export -o output/wrapped_filter_nextflow/bin
# $JAVA -f atoms/wrapped_filter/functionality.yaml -p atoms/wrapped_filter/platform_nextflow.yaml export -o output/wrapped_filter_nextflow

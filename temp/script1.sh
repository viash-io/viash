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


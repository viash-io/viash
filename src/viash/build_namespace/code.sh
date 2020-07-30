#!/bin/bash

# VIASH START
par_namespace="viash"
par_src="src"
par_target="target"
# VIASH END

red=`tput setaf 1`
green=`tput setaf 2`
blue=`tput setaf 4`
reset=`tput sgr0`

for f in `find $par_src/$par_namespace -name functionality.yaml`; do

  tool_dir=`dirname $f`
  tool_name=`basename $tool_dir`
  ns_dir=`dirname $tool_dir`
  ns_name=`basename $ns_dir`

  echo "${green}Processing $tool_name in namespace $ns_name${reset}"

  echo "- ${blue}Native${reset}"
  if [ -f $tool_dir/platform_native.yaml ]; then
    echo "  Platform file found in $tool_dir"
    viash export \
          -f $tool_dir/functionality.yaml \
          -p $tool_dir/platform_native.yaml \
          -o $par_target/native/$ns_name/$tool_name
    cp $par_target/native/$ns_name/$tool_name/$tool_name "$par_target/${ns_name}_${tool_name}"
  elif [ -f platform/native.yaml ]; then
    echo "  Platform file found in platform/"
    viash export \
          -f $tool_dir/functionality.yaml \
          -p platform/native.yaml \
          -o $par_target/native/$ns_name/$tool_name
    cp $par_target/native/$ns_name/$tool_name/$tool_name "$par_target/${ns_name}_${tool_name}"
  else
    echo "  No platform file found"
  fi

  echo "- ${blue}Docker${reset}"
  if [ -f $tool_dir/platform_docker.yaml ]; then
    echo "  Platform file found in $tool_dir"
    viash export \
          -f $tool_dir/functionality.yaml \
          -p $tool_dir/platform_docker.yaml \
          -o $par_target/docker/$ns_name/$tool_name
    cp $par_target/docker/$ns_name/$tool_name/$tool_name "$par_target/${ns_name}_${tool_name}"
  elif [ -f platform/docker.yaml ]; then
    echo "  Platform file found in platform/"
    viash export \
          -f $tool_dir/functionality.yaml \
          -p platform/docker.yaml \
          -o $par_target/docker/$ns_name/$tool_name
    cp $par_target/docker/$ns_name/$tool_name/$tool_name "$par_target/${ns_name}_${tool_name}"
  else
    echo "  No platform file found"
  fi

  echo "- ${blue}NextFlow${reset}"
  if [ -f $tool_dir/platform_nextflow.yaml ]; then
    echo "  Platform file found in $tool_dir"
    viash export \
          -f $tool_dir/functionality.yaml \
          -p $tool_dir/platform_nextflow.yaml \
          -o $par_target/modules/$ns_name/$tool_name
  elif [ -f platform/nextflow.yaml ]; then
    echo "  Platform file found in platform"
    viash export \
          -f $tool_dir/functionality.yaml \
          -p platform/nextflow.yaml \
          -o $par_target/modules/$ns_name/$tool_name
  else
    echo "  No platform file found"
  fi

done



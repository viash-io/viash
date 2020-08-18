#!/bin/bash

# VIASH START
par_namespace="viash"
par_src="src"
par_target="target"
par_withdocker="false"
# VIASH END

red=`tput setaf 1`
green=`tput setaf 2`
blue=`tput setaf 4`
reset=`tput sgr0`

viash="echo"
if [ "$par_withdocker" = "true" ]; then
  viash="docker run -it test-viash viash"
else
  viash="viash"
fi

function ViashBuildNamespaceForPlatform {
  file="$1"
  platform="$2"
  
  if [ $platform = "nextflow" ]; then
    platform_out="modules"
  else
    platform_out="$platform"
  fi
  
  tool_dir=`dirname "$file"`
  tool_name=`basename "$tool_dir"`
  ns_dir=`dirname "$tool_dir"`
  ns_name=`basename "$ns_dir"`
  
  echo "- ${blue}${platform}${reset}"
  if [ -f $tool_dir/platform_${platform}.yaml ]; then
    echo "  Platform file found in $tool_dir"
    "$viash" export \
      -f "${tool_dir}/functionality.yaml" \
      -p "${tool_dir}/platform_${platform}.yaml" \
      -o "${par_target}/${platform_out}/${ns_name}/${tool_name}"
    
    if [ $platform != "nextflow" ]; then
      cp ${par_target}/${platform_out}/${ns_name}/${tool_name}/${tool_name} "${par_target}/${ns_name}-${tool_name}"
    fi
  elif [ -f platform/${platform}.yaml ]; then
    echo "  Platform file found in platform/"
    "$viash" export \
      -f "${tool_dir}/functionality.yaml" \
      -p platform/${platform}.yaml \
      -o "${par_target}/${platform_out}/${ns_name}/${tool_name}"
    
    if [ $platform != "nextflow" ]; then
      cp ${par_target}/${platform_out}/${ns_name}/${tool_name}/${tool_name} "${par_target}/${ns_name}-${tool_name}"
    fi
  else
    echo "  No platform file found"
  fi
}

for f in `find $par_src/$par_namespace -name functionality.yaml`; do
  echo "${green}Processing $tool_name in namespace $ns_name${reset}"
  
  ViashBuildNamespaceForPlatform "$f" native
  ViashBuildNamespaceForPlatform "$f" docker
  ViashBuildNamespaceForPlatform "$f" nextflow
done



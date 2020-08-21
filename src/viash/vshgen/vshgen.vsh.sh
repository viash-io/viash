#' functionality:
#'   name: vshgen
#'   version: 1.0
#'   description: |
#'     Merge yaml files into the new vsh format.
#'   arguments:
#'   - name: "--functionality"
#'     alternatives: ["-f"]
#'     type: file
#'     description: The functionality meta-data.
#'     default: functionality.yaml
#'     must_exist: true
#'   - name: "--platform"
#'     alternatives: ["-p"]
#'     type: file
#'     description: The platform meta-data (zero or more).
#'     multiple: true
#'     default: "platform_native.yaml:platform_docker.yaml:platform_nextflow.yaml"
#'   - name: "--output"
#'     alternatives: ["-o"]
#'     type: file
#'     description: The path to the output file.
#'     direction: output
#'   - name: "--rm"
#'     type: boolean_true
#'     description: Remove the source files after use.
#' platforms:
#' - type: docker
#'   image: mikefarah/yq
#'   apk: 
#'     packages:
#'     - bash

set -e

#### MAKE VARIABLES
script_path="`dirname $par_functionality`/`yq read $par_functionality 'resources.[0].path'`"
if [ -z "$par_output" ]; then
  par_output=`echo $script_path | sed 's#\(.*\)\(\.[^\.]*\)#\1.vsh\2#'`
fi

# echo par_functionality: $par_functionality

echo "Merging viash files into one!"

#### FUNCTIONALITY
echo "> Merging [`basename $par_functionality`] into [`basename $par_output`]"
echo "#' functionality:" > "$par_output"
yq delete $par_functionality 'resources.[0]' | sed "s/^/#'   /" >> "$par_output"
to_rm="\"$par_functionality\""

#### PLATFORM(S)
if [ ! -z "$par_platform" ]; then
  echo "#' platforms:" >> "$par_output"
  
  found_platforms=false
  IFS=":"
  for plat in $par_platform; do
    if [ -f "$plat" ]; then
      echo "> Merging [`basename $plat`] into [`basename $par_output`]"
      yq delete "$plat" 'volumes' | sed "s/^/#'   /" | sed "s/#'   type:/#' - type:/" >> "$par_output"
      to_rm="\"$plat\" $to_rm"
      found_platforms=true
    fi
  done
  unset IFS
  
  if [ "$found_platforms" = "false" ]; then
    echo "  - type: native"
  fi
fi

#### SCRIPT
echo "> Merging [`basename $script_path`] into [`basename $par_output`]"
awk "/VIASH START/,/VIASH END/ { next; }; 1 {print; }" "$script_path" >> "$par_output"
to_rm="\"$script_path\" $to_rm"

#### CLEANUP
if [ "$par_rm" = "true" ]; then
  echo -n "> Removing source files:"
  for fil in $to_rm; do
    echo -n " [`eval basename $fil`]"
  done
  echo
  eval rm $to_rm
fi

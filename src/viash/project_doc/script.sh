#!/bin/bash

function output {
  echo "$@" >> $par_output
}

echo "# Repository Overview" > $par_output
output ""

namespaces=`viash ns list -s $par_repo/"$par_src" | yq e '.[].functionality.namespace' - | uniq`

tempscript=$(mktemp ".tmp_viash_config_view_XXXXXX")
function clean_up {
  [[ -f "$tempscript" ]] && rm "$tempscript"
}
trap clean_up EXIT
    
for ns in $namespaces; do
  echo "> Generating documentation for namespace $ns"
  output "## $ns"
  output ""
  comp_files=`viash ns list -n $ns -s $par_repo/$par_src | yq e '.[].info.config' - | uniq`
  for comp_file in $comp_files; do
    viash config view "$comp_file" > "$tempscript"
    
    comp_name=`yq e '.functionality.name' "$tempscript"`
    comp_desc=`yq e '.functionality.description' "$tempscript"`
    echo "  > Generating documentation for $ns/$comp_name"
    output "### $comp_name"
    output ""
    output $comp_desc
    output ""
    output '```sh'
    output "$ viash run $comp_file -- -h"
    viash run $comp_file -- -h >> $par_output
    output '```'
    output ""
    
    clean_up
  done
done

output "# Tests"
output ""
output "__TODO__"
output ""

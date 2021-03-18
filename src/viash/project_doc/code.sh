#!/bin/bash

function output {
  echo "$@" >> $par_output
}

echo "# Repository Overview" > $par_output
output ""

namespaces=`viash ns list -s $par_repo/"$par_src" | yq e '.configs[].functionality.namespace' - | uniq`

for ns in $namespaces; do
  echo "> Generating documentation for namespace $ns"
  output "## $ns"
  output ""
  components=`viash ns list -n $ns -s $par_repo/$par_src | yq e '.configs[].functionality.name' - | uniq`
  for comp in $components; do
    echo "  > Generating documentation for $ns/$comp"
    output "### $comp"
    output ""
    output `viash ns list -q "$comp" -s $par_repo/$par_src | yq e '.configs[0].functionality.description' -`
    output ""
    output '```sh'
    output "$ viash run $par_repo/$par_src/$ns/$comp/*.vsh.yaml -- -h"
    viash run $par_repo/$par_src/$ns/$comp/*.vsh.yaml -- -h >> $par_output
    output '```'
    output ""
  done
done

output "# Tests"
output ""
output "__TODO__"
output ""

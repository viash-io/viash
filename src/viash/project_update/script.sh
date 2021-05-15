#!/bin/bash

# get the root of the repository
REPO_ROOT=`pwd`

if [ ! -d "$par_bin" ]; then
  echo "> Creating $par_bin"
  mkdir "$par_bin"
fi

# remove previous binaries
echo "> Cleanup"
if [ -f $par_bin/viash* ]; then
  echo "  > Removing previous versions of viash"
  rm $par_bin/viash*
fi
if [ -f $par_bin/project_update ]; then
  echo "  > Removing previous versions of project binaries"
  rm $par_bin/project_*
fi
if [ -f $par_bin/skeleton ]; then
  echo "  > Removing previous versions of skeleton binary"
  rm $par_bin/skeleton*
fi

# build helper components
build_dir=$(mktemp -d)
function clean_up {
  [[ -d "$build_dir" ]] && rm -r "$build_dir"
}
trap clean_up EXIT

# download latest viash components
echo "> Fetching components sources"
fetch --repo="https://github.com/data-intuitive/viash" --branch="develop" --source-path="/src/viash" "$build_dir"

# build components
echo "> Building components"
viash ns build \
  -s "$build_dir" \
  -t "$par_bin" \
  --flatten \
  -c '.functionality.arguments[.name == "--registry"].default := "'$par_registry'"' \
  -c '.functionality.arguments[.name == "--viash"].default := "'$par_viash'"' \
  -c '.functionality.arguments[.name == "--log" && root.functionality.name == "project_test"].default := "docs/viash_ns_test_output.tsv"' \
  -c '.functionality.arguments[.name == "--namespace_separator"].default := "'$par_namespace_separator'"'

# copy viash itself
echo "> Install viash under $par_bin"
cp `which viash` "$par_bin"

echo "> Done, happy viash-ing!"

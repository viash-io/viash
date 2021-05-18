#!/bin/bash

# get the root of the repository
REPO_ROOT=`pwd`

if [ ! -d "$par_bin" ]; then
  echo "> Creating $par_bin"
  mkdir "$par_bin"
fi

cd "$par_bin"

# Retrieving version
viash_version=`viash -v | sed -E 's/^viash ([v0-9.]+[\-rc0-9]*).*/\1/'`
if [ -z $par_version ]; then
  par_version="$viash_version"
  same_version=1
else
  same_version=0
fi
echo "> Using version $par_version"

# remove previous binaries
echo "> Cleanup"
if [ -f viash* ]; then
  echo "  > Removing previous versions of viash"
  rm viash*
fi
if [ -f project_update ]; then
  echo "  > Removing previous versions of project binaries"
  rm project_*
fi
if [ -f skeleton ]; then
  echo "  > Removing previous versions of skeleton binary"
  rm skeleton*
fi

# build helper components
build_dir=$(mktemp -d)
function clean_up {
  [[ -d "$build_dir" ]] && rm -r "$build_dir"
}
trap clean_up EXIT

# Install viash itself
echo "> Install viash $par_version under $par_bin"
if [ $same_version = 1 ];then
  cp `which viash` .
else
  wget -nv "https://github.com/data-intuitive/viash/releases/download/$par_version/viash"
  chmod +x viash
fi

# download latest viash components
echo "> Fetching components sources"
fetch --repo="https://github.com/data-intuitive/viash" --branch="$par_version" --source-path="/src/viash" "$build_dir"

# build components
echo "> Building components"
./viash ns build \
  -s "$build_dir" \
  -t . \
  --flatten \
  -c '.functionality.arguments[.name == "--registry"].default := "'$par_registry'"' \
  -c '.functionality.arguments[.name == "--viash"].default := "'$par_viash'"' \
  -c '.functionality.arguments[.name == "--log" && root.functionality.name == "project_test"].default := "docs/viash_ns_test_output.tsv"' \
  -c '.functionality.arguments[.name == "--namespace_separator"].default := "'$par_namespace_separator'"'

echo "> Done, happy viash-ing!"

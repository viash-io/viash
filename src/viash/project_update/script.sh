#!/bin/bash

# get the root of the repository
REPO_ROOT=$par_pwd

# ensure that the command below is run from the root of the repository
cd "$REPO_ROOT"

# remove previous binaries
echo "> Cleanup"
rm bin/viash*
rm bin/project_*
rm bin/skeleton*

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
  -t bin \
  --flatten \
  -c '.functionality.arguments[.name == "--registry"].default := "'$par_registry'"' \
  -c '.functionality.arguments[.name == "--viash"].default := "'$par_viash'"' \
  -c '.functionality.arguments[.name == "--log" && root.functionality.name == "project_test"].default := "docs/viash_ns_test_output.tsv"' \
  -c '.functionality.arguments[.name == "--namespace_separator"].default := "'$par_namespace_separator'"'

# copy viash itself
echo "> Install viash under bin/"
cp `which viash` bin/

echo "> Done, happy viash-ing!"

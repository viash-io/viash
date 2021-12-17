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
if [ -z $par_tag ]; then
  par_tag="$viash_version"
  same_version=1
else
  same_version=0
fi
echo "> Using tag $par_tag"

# remove previous binaries
echo "> Cleanup"
if [ -f viash ]; then
  echo "  > Removing previous versions of viash and recent project binaries"
  rm viash*
fi
if [ -f project_update ]; then
  echo "  > Removing previous versions of project binaries"
  rm project_*
fi
if [ -f skeleton ]; then
  echo "  > Removing previous versions of skeleton binary"
  rm skeleton
fi

# build helper components
build_dir=$(mktemp -d)
function clean_up {
  [[ -d "$build_dir" ]] && rm -r "$build_dir"
}
trap clean_up EXIT

# Install viash itself
echo "> Install viash $par_tag under $par_bin"
if [ $same_version = 1 ];then
  cp `which viash` .
elif [ $par_tag == "develop" ]; then
  cd $build_dir
  git clone --branch develop https://github.com/viash-io/viash
  cd $build_dir/viash
  ls
  ./configure
  make bin/viash
  cp bin/viash $par_bin
  cd ..
  rm -r viash
  cd $par_bin
else
  wget -nv "https://github.com/viash-io/viash/releases/download/$par_tag/viash"
  chmod +x viash
fi

# download viash components
echo "> Fetching components sources (version $par_tag)"
fetch --repo="https://github.com/viash-io/viash" --branch="$par_tag" --source-path="/src/viash" "$build_dir"

# build components
echo "> Building components"
./viash ns build \
  -s "$build_dir" \
  -t . \
  --flatten \
  -c '.functionality.arguments[.name == "--registry"].default := "'$par_registry'"' \
  -c '.functionality.arguments[.name == "--viash"].default := "'$par_viash'"' \
  -c '.functionality.arguments[.name == "--log" && root.functionality.name == "viash_test"].default := "'$par_log'"' \
  -c '.functionality.arguments[.name == "--namespace_separator"].default := "'$par_namespace_separator'"'

echo "> Done, happy viash-ing!"

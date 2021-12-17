#!/bin/bash

if ! command -v unzip &> /dev/null; then
    echo "unzip needs to be installed"
    exit
fi
if ! command -v wget &> /dev/null; then
    echo "wget needs to be installed"
    exit
fi


# get the root of the repository
REPO_ROOT=`pwd`

if [ ! -d "$par_bin" ]; then
  echo "> Creating $par_bin"
  mkdir "$par_bin"
fi

echo "> Using tag $par_tag"

# remove previous binaries
echo "> Cleanup"
if [ -f "$par_bin/viash" ]; then
  echo "  > Removing previous versions of Viash and recent project binaries"
  rm "$par_bin/viash*"
fi
if [ -f "$par_bin/project_update" ]; then
  echo "  > Removing previous versions of project binaries"
  rm "$par_bin/project_*"
fi
if [ -f "$par_bin/skeleton" ]; then
  echo "  > Removing previous versions of skeleton binary"
  rm "$par_bin/skeleton"
fi


# make temporary dir for building things
build_dir=$(mktemp -d)
function clean_up {
  [[ -d "$build_dir" ]] && rm -r "$build_dir"
}
trap clean_up EXIT


if [ $par_tag == "develop" ]; then

  if ! command -v sbt &> /dev/null; then
      echo "sbt needs to be installed"
      exit
  fi

  # Download Viash helper scripts
  echo "> Downloading source (version $par_tag)"
  wget -nv "https://github.com/viash-io/viash/archive/refs/heads/$par_tag.zip" -O "$build_dir/$par_tag.zip"
  unzip -q "$build_dir/$par_tag.zip" -d "$build_dir"

  # Build Viash
  echo "> Building Viash from source"
  cd "$build_dir/viash-$par_tag"
  ./configure
  make bin/viash
  cp bin/viash "$par_bin"
  cd "$REPO_ROOT"
else
  # Download Viash
  echo "> Downloading Viash v$par_tag under $par_bin"
  wget -nv "https://github.com/viash-io/viash/releases/download/$par_tag/viash" -O "$par_bin/viash"
  chmod +x "$par_bin/viash"

  # Download Viash helper scripts
  echo "> Downloading source v$par_tag"
  wget -nv "https://github.com/viash-io/viash/archive/refs/tags/$par_tag.zip" -O "$build_dir/$par_tag.zip"
  unzip -q "$build_dir/$par_tag.zip" -d "$build_dir"
fi

# build components
echo "> Building Viash helper script"
"$par_bin/viash" ns build \
  -s "$build_dir/viash-$par_tag/src/viash" \
  -t "$par_bin/" \
  --flatten \
  -c '.functionality.arguments[.name == "--registry"].default := "'$par_registry'"' \
  -c '.functionality.arguments[.name == "--viash"].default := "'$par_viash'"' \
  -c '.functionality.arguments[.name == "--log" && root.functionality.name == "viash_test"].default := "'$par_log'"' \
  -c '.functionality.arguments[.name == "--namespace_separator"].default := "'$par_namespace_separator'"'

echo "> Done, happy viash-ing!"

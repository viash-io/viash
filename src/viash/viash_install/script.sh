#!/bin/bash

## VIASH START
par_destination="testdir/"
par_tag="latest"
## VIASH END

if ! command -v unzip &> /dev/null; then
    echo "unzip needs to be installed"
    exit
fi
if ! command -v curl &> /dev/null; then
    echo "curl needs to be installed"
    exit
fi

set -e

# get the root of the repository
REPO_ROOT=`pwd`

if [ ! -d "$par_destination" ]; then
  echo "> Creating $par_destination"
  mkdir "$par_destination"
fi

if [ "$par_tag" == "latest" ]; then
  LATEST_RELEASE=$(curl -L -s -H 'Accept: application/json' https://github.com/viash-io/viash/releases/latest)
  par_tag=$(echo $LATEST_RELEASE | sed -e 's/.*"tag_name":"\([^"]*\)".*/\1/')
  echo "> Detected latest version as $par_tag"
fi

echo "> Using tag $par_tag"

# remove previous binaries
echo "> Cleanup"
if [ -f "$par_destination/$par_name" ]; then
  echo "> Removing previous versions of Viash and recent project binaries"
  rm "$par_destination/$par_name"
fi

# make temporary dir for building things
build_dir=$(mktemp -d)
function clean_up {
  [[ -d "$build_dir" ]] && rm -r "$build_dir"
}
trap clean_up EXIT

if [[ "$par_tag" == "develop" || "$par_tag" =~ ^@.*$ ]]; then
  used_tag=${par_tag#@}

  # Download Viash helper scripts
  echo "> Downloading Viash source code @$used_tag"
  curl -L -s "https://github.com/viash-io/viash/archive/refs/heads/$used_tag.zip" -o "$build_dir/$used_tag.zip"
  unzip -q "$build_dir/$used_tag.zip" -d "$build_dir"

  # Build Viash
  echo "> Building Viash from source"
  cd "$build_dir/viash-$used_tag"
  ./configure

  if ! command -v sbt &> /dev/null; then
      echo "WARNING: sbt could not be found, using a Docker backend to build Viash from source instead. Install sbt to remove this warning."

      if ! command -v docker &> /dev/null; then
          echo "docker needs to be installed"
          exit
      fi

      make with-docker
  else
      make bin/viash
  fi
  
  cd "$REPO_ROOT"
  cp "$build_dir/viash-$used_tag/bin/viash" "$par_destination/$par_name"
else
  # Download Viash
  echo "> Downloading Viash v$par_tag under $par_destination and naming it $par_name"
  curl -L -s "https://github.com/viash-io/viash/releases/download/$par_tag/viash" -o "$par_destination/$par_name"
  chmod +x "$par_destination/$par_name"
fi

echo "> Done, happy viash-ing!"

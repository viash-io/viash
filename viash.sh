#!/bin/sh

echo
echo "██╗   ██╗██╗ █████╗ ███████╗██╗  ██╗"
echo "██║   ██║██║██╔══██╗██╔════╝██║  ██║"
echo "██║   ██║██║███████║███████╗███████║"
echo "╚██╗ ██╔╝██║██╔══██║╚════██║██╔══██║"
echo " ╚████╔╝ ██║██║  ██║███████║██║  ██║"
echo "  ╚═══╝  ╚═╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝"
echo

VIASH_DIR=$(dirname "$0")
VERSION=$(cat "$VIASH_DIR/build.sbt" | grep "version" | sed 's/.*"\(.*\)"$/\1/')
# This is a hack and a manual step.
# TODO: replace with the release assembly when ready
SOURCE="$VIASH_DIR/atoms/viash/viash-assembly-$VERSION.jar"

echo "Installing Viash in ~/bin, make sure it's in your \$PATH..."

if [ -d ~/bin ]; then
  java -jar "$SOURCE" \
    export \
    --functionality "$VIASH_DIR/atoms/viash/functionality.yaml" \
    --platform "$VIASH_DIR/atoms/viash/platform_native.yaml" \
    --output ~/bin
else
  echo "OEPS, ~/bin directory does not exist!"
fi

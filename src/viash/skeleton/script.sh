#!/bin/bash

cdir="$par_src/$par_namespace/$par_name"
mkdir -p "$cdir"
cp "$VIASH_RESOURCES_DIR/skeleton.yaml" "$cdir/config.vsh.yaml"
cp "$VIASH_RESOURCES_DIR/skeleton.sh" "$cdir/script.sh"

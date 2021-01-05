#!/usr/bin/env bash

# VIASH START
par_which=""
par_file=""
resources_dir="."
# VIASH END

set -e

if [ ! -z "$par_which" ]; then
  which $par_which
fi

if [ ! -z "$par_file" ]; then
  if [ -f "$par_file" ]; then
    echo "$par_file exists."
  else
    echo "$par_file doesn't exist."
  fi
fi
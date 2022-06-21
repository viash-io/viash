#!/usr/bin/env bash

# VIASH START
par_which=""
par_file=""
resources_dir="."
# VIASH END

#set -e

RES=`which fortune`

echo "fortune: $RES"

if [ -n "$RES" ]; then
  echo "fortune found."
  exit 0
else
  echo "fortune not found."
  exit 1
fi
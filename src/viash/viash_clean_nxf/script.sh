#!/bin/bash

add=""

if [ ! -z "$par_after" ]; then
  add="$add -after $par_after"
fi

if [ ! -z "$par_before" ]; then
  add="$add -before $par_before"
fi

nextflow clean -f "$add"

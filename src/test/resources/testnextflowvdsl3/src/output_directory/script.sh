#!/bin/test

[ -d "$par_output" ] && rm "$par_output"

mkdir -p "$par_output"
echo "test123" > "${par_output}/test1.txt"
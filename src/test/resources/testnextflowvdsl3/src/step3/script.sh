#!/bin/test

tmpfile=$(mktemp)
function clean_up {
  rm -rf "$tmpfile"
}
trap clean_up EXIT


if [ ! -z "$par_input" ]; then
  IFS=";"
  for var in $par_input; do
    unset IFS
    cat "$var" >> "$tmpfile"
  done
fi

wc -l "$tmpfile" > "$par_output"

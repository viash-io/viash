#!/bin/test

tmpfile=$(mktemp)
function clean_up {
  rm -rf "$tmpfile"
}
trap clean_up EXIT


if [ ${#par_input[@]} -gt 0 ]; then
  for var in "${par_input[@]}"; do
    cat "$var" >> "$tmpfile"
  done
fi

wc -l "$tmpfile" > "$par_output"

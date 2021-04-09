#!/bin/bash

cat "$par_input" > "$par_output"
cat "$par_input" | sed "s#.*#log - &#" > "$par_log"

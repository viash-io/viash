#!/bin/bash

mkdir $par_output

cat "$par_input" > "$par_output"/first_output_file
cat "$par_input" | sed "s#.*#log - &#" > "$par_output"/second_output_file

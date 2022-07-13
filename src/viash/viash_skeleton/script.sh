#!/bin/bash


# check par_language
if [[ $par_language =~ ^bash|sh|Bash$ ]]; then
  script_lang=bash
elif [[ $par_language =~ ^r|R$ ]]; then
  script_lang=r
elif [[ $par_language =~ ^py|python|Python$ ]]; then
  script_lang=python
else 
  echo "Unrecognised language: $par_language; please specify one of 'python', 'r', or 'bash'"
  exit 1
fi

# create output dir
out_dir="$par_src/$par_namespace/$par_name"
mkdir -p "$out_dir"

##################################################################################
###                                FUNCTIONALITY                               ###
##################################################################################

# write header
cat > "$out_dir/config.vsh.yaml" << HERE
functionality:
  name: "$par_name"
HERE

# write namespace, if need be
if [ ! -z "$par_namespace" ]; then
cat >> "$out_dir/config.vsh.yaml" << HERE
  namespace: "$par_namespace"
HERE
fi

# write more metadata and initial arguments
cat >> "$out_dir/config.vsh.yaml" << HERE
  version: 0.0.1
  description: |
    Replace this with a (multiline) description of your component.
  arguments:
    - name: "--input"
      alternatives: [ "-i" ]
      type: file
      required: true
      description: Describe the input file.
      example: input.txt
    - name: "--output"
      alternatives: [ "-o" ]
      type: file
      direction: output
      required: true
      description: Describe the output file.
      example: output.txt
    - name: "--option"
      type: string
      description: Describe an optional parameter.
      default: "default-"
HERE

##################################################################################
###                                 BASH SCRIPTS                               ###
##################################################################################
if [ $script_lang == "bash" ]; then
cat >> "$out_dir/config.vsh.yaml" << HERE
  resources:
    - type: bash_script
      path: script.sh
  test_resources:
    - type: bash_script
      path: test.sh
HERE

cat >> "$out_dir/script.sh" << 'HERE'
#!/bin/bash

echo "This is a skeleton component"
echo "The arguments are:"
echo " - input:  $par_input"
echo " - output: $par_output"
echo " - option: $par_option"
echo

echo "Writing output file"
cat "$par_input" | sed "s#.*#$par_option-&#" > "$par_output"
HERE

cat >> "$out_dir/test.sh" << MAJORHERE
#!/bin/bash

set -ex

echo ">>> Creating dummy input file"
cat > input.txt << HERE
one
two
three
HERE

echo ">>> Running executable"
./$par_name --input input.txt --output output.txt --option FOO

echo ">>> Checking whether output file exists"
[[ ! -f output.txt ]] && echo "Output file could not be found!" && exit 1

# create expected output file
cat > expected_output.txt << HERE
FOO-one
FOO-two
FOO-three
HERE

echo ">>> Checking whether content matches expected content"
diff output.txt expected_output.txt
[ \$? -ne 0 ] && echo "Output file did not equal expected output" && exit 1

# print final message
echo ">>> Test finished successfully"

# do not remove this
# as otherwise your test might exit with a different exit code
exit 0
MAJORHERE

##################################################################################
###                                 RLANG SCRIPTS                              ###
##################################################################################
elif [ $script_lang == "r" ]; then
cat >> "$out_dir/config.vsh.yaml" << HERE
  resources:
    - type: r_script
      path: script.R
  test_resources:
    - type: r_script
      path: test.R
HERE
cat >> "$out_dir/script.R" << 'HERE'
cat("This is a skeleton component\n")
cat("The arguments are:\n")
cat(" - input: ", par$input, "\n", sep = "")
cat(" - output: ", par$output, "\n", sep = "")
cat(" - option: ", par$option, "\n", sep = "")
cat("\n")

cat("Reading input file\n")
lines <- readLines(par$input)

cat("Running output algorithm\n")
new_lines <- paste0(par$option, "-", lines)

cat("Writing output file\n")
writeLines(new_lines, con = par$output)
HERE

cat >> "$out_dir/test.R" << HERE
library(testthat)

# create dummy input file
old_lines <- c("one", "two", "three")
writeLines(old_lines, "input.txt")

# run executable
system("./$par_name --input input.txt --output output.txt --option FOO")

# check whether output file exists
expect_true(file.exists("output.txt"))

# check whether content matches expected content
expected_lines <- c("FOO-one", "FOO-two", "FOO-three")
new_lines <- readLines("output.txt")
expect_equal(new_lines, expected_lines)

cat(">>> Test finished successfully!")
HERE

##################################################################################
###                                PYTHON SCRIPTS                              ###
##################################################################################
elif [ $script_lang == "python" ]; then
cat >> "$out_dir/config.vsh.yaml" << HERE
  resources:
    - type: python_script
      path: script.py
  test_resources:
    - type: python_script
      path: test.py
HERE

cat >> "$out_dir/script.py" << 'HERE'
print("This is a skeleton component")
print("The arguments are:")
print(" - input: ", par["input"])
print(" - output: ", par["output"])
print(" - option: ", par["option"])
print("")


with open(par["input"], "r") as reader, open(par["output"], "w") as writer:
    lines = reader.readlines()
    
    new_lines = [par["option"] + x for x in lines]
    
    writer.writelines(new_lines)
HERE

cat >> "$out_dir/test.py" << HERE
import unittest
import os
from os import path
import subprocess

print(">> Writing test file")
with open("input.txt", "w") as writer:
    writer.writelines(["one\n", "two\n", "three\n"])

print(">> Running component")
out = subprocess.check_output(["./$par_name", "--input", "input.txt", "--output", "output.txt", "--option", "FOO-"]).decode("utf-8")

print(">> Checking whether output file exists")
assert path.exists("output.txt")

print(">> Checking contents of output file")
with open("output.txt", "r") as reader:
    lines = reader.readlines()
assert lines == ["FOO-one\n", "FOO-two\n", "FOO-three\n"]

print(">> All tests succeeded successfully!")
HERE

fi

##################################################################################
###                                  PLATFORMS                                 ###
##################################################################################
# write platforms
cat >> "$out_dir/config.vsh.yaml" << HERE
platforms:
HERE

# iterate over different specified platforms
IFS=','
set -f
for platform in $par_platform; do
  unset IFS
  if [ $platform == "docker" ]; then
  
    # choose different default docker image based on language
    if [ $script_lang == "bash" ]; then
    cat >> "$out_dir/config.vsh.yaml" << HERE
  - type: docker
    image: ubuntu:20.04
    setup:
      - type: apt
        packages: 
          - bash
HERE

    elif [ $script_lang == "r" ]; then
    cat >> "$out_dir/config.vsh.yaml" << HERE
  - type: docker
    image: rocker/tidyverse:4.0.4
    setup:
      - type: r
        packages: 
          - princurve
HERE

    elif [ $script_lang == "python" ]; then
    cat >> "$out_dir/config.vsh.yaml" << HERE
  - type: docker
    image: python:3.9.3-buster
    setup:
      - type: python
        packages: 
          - numpy
HERE
    fi
  
  elif [ $platform == "native" ]; then
    cat >> "$out_dir/config.vsh.yaml" << HERE
  - type: native
HERE
  
  elif [ $platform == "nextflow" ]; then
    cat >> "$out_dir/config.vsh.yaml" << HERE
  - type: nextflow
HERE

  fi
done
set +f



  




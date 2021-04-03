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
    - name: "--output"
      alternatives: [ "-o" ]
      type: file
      direction: output
      required: true
      description: Describe the output file.
    - name: "--option"
      type: string
      description: Describe an optional parameter.
      default: "default-"
HERE

# write resources
if [ $script_lang == "bash" ]; then
cat >> "$out_dir/config.vsh.yaml" << HERE
  resources:
    - type: bash_script
      path: script.sh
  tests:
    - type: bash_script
      path: test.sh
HERE
  cp "$resources_dir/template_script.sh" "$out_dir/script.sh"
  cat "$resources_dir/template_test.sh" | sed "s#EXECUTABLE#$par_name#" > "$out_dir/test.sh"

elif [ $script_lang == "r" ]; then
cat >> "$out_dir/config.vsh.yaml" << HERE
  resources:
    - type: r_script
      path: script.R
  tests:
    - type: r_script
      path: test.R
HERE
  cp "$resources_dir/template_script.R" "$out_dir/script.R"
  cat "$resources_dir/template_test.R" | sed "s#EXECUTABLE#$par_name#" > "$out_dir/test.R"

elif [ $script_lang == "python" ]; then
cat >> "$out_dir/config.vsh.yaml" << HERE
  resources:
    - type: python_script
      path: script.py
  tests:
    - type: python_script
      path: test.py
HERE
  cp "$resources_dir/template_script.py" "$out_dir/script.py"
  cat "$resources_dir/template_test.py" | sed "s#EXECUTABLE#$par_name#" > "$out_dir/test.py"
fi

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



  




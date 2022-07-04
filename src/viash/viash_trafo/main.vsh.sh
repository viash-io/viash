#' functionality:
#'   name: viash_trafo
#'   namespace: viash
#'   version: 1.0
#'   description: |
#'     Transform viash formats.
#'   arguments:
#'   - name: "--input"
#'     alternatives: ["-i"]
#'     type: file
#'     description: Input file
#'     must_exist: false
#'     required: true
#'   - name: "--output_dir"
#'     alternatives: ["-o"]
#'     type: file
#'     description: Output directory
#'     direction: output
#'     required: true
#'   - name: "--format"
#'     alternatives: ["-f"]
#'     type: string
#'     description: Output format. Must be one of 'script', 'config'
#'     required: true
#'   - name: "--rm"
#'     type: boolean_true
#'     description: Remove the source files after use.
#'   test_resources:
#'   - type: bash_script
#'     path: tests/test_conversion.sh
#'   - path: tests/config.novsh.yaml
#'     dest: config.vsh.yaml
#'   - path: tests/script.sh
#'   - path: tests/script.novsh.sh
#'     dest: script.vsh.sh
#' platforms:
#' - type: docker
#'   image: bash:4.0
#'   setup: 
#'     - type: docker
#'       run: 
#'         - wget https://github.com/mikefarah/yq/releases/download/3.4.0/yq_linux_amd64 -O /usr/bin/yq && chmod +x /usr/bin/yq
#' - type: native

set -e

# detect input type
input_dir=$(dirname $par_input)
if [[ "$par_input" =~ ^.*\.vsh\.(sh|r|R|py)$ ]]; then
  input_type=script
  input_ext=`echo "$par_input" | sed 's#.*\.##'`
  
  if [ "$input_ext" = "sh" ]; then
    script_type="bash"
  elif [[ $input_ext =~ ^[rR]$ ]]; then
    script_type="r"
  elif [ "$input_ext" = "py" ]; then
    script_type="python"
  else
    echo "Unsupported format: $input_ext!"
    exit 1
  fi
elif [[ "$par_input" =~ ^.*\.vsh\.(yaml|yml)$ ]]; then
  input_type=config
else
  echo Input: unsupported format.
  exit 1
fi

# create dir if it does not exist
[[ -d "$par_output_dir" ]] || mkdir -p "$par_output_dir"

# check format
if [[ ! $par_format =~ ^(script|config)$ ]]; then
  echo "Output: unsupported format. Must be one of 'script' or 'config'"
  exit 1
fi

# ------------------------ X -> X ------------------------
if [ $input_type = $par_format ]; then 
  echo Input type is equal to output type. 
  echo Just use cp, you son of a silly person.
  cp "$par_input" "$par_output_dir/$(basename $par_input)"

# ------------------------ SCRIPT -> CONFIG ------------------------
elif [ $input_type = "script" ] && [ $par_format = "config" ]; then
  echo "Converting from 'script' to 'config'"

  # determine output paths
  config_yaml_relative="config.vsh.yaml"
  config_yaml_path="$par_output_dir/$config_yaml_relative"
  output_script_relative="$(basename $par_input | sed 's#\.vsh\.#.#')"
  output_script_path="$par_output_dir/$output_script_relative"
  
  # WRITING CONFIG YAML
  echo "> Writing config yaml to $config_yaml_relative"
  CONFIG_YAML=$(cat "$par_input" | grep "^#' " | sed "s/^#' //")
  
  # write yaml without resources
  echo "$CONFIG_YAML" | yq d - functionality.resources > "$config_yaml_path"
  
  # add script to resources
  printf "functionality:\n  resources:\n  - type: ${script_type}_script\n    path: $output_script_relative\n" | yq m "$config_yaml_path" - -i
  
  # add other resources
  has_resources=`echo "$CONFIG_YAML" | yq read - functionality.resources | head -1`
  if [ ! -z "$has_resources" ]; then
    echo "$CONFIG_YAML" | yq read - functionality.resources | yq p - functionality.resources | yq m -a append "$config_yaml_path" - -i
  fi

  # WRITING SCRIPT
  echo "> Writing script to $output_script_relative"
  cat "$par_input" | grep -v "^#' " > "$output_script_path"
  

# ------------------------ CONFIG -> SCRIPT ------------------------
elif [ $input_type = "config" ] && [ $par_format = "script" ]; then
  echo "Converting from 'config' to 'script'"

  # determine output paths
  input_script_relative=$(yq read "$par_input" 'functionality.resources.[0].path')
  input_script_path="$input_dir/$input_script_relative"
  output_script_relative=$(echo "$input_script_relative" | sed 's#\(\.[^\.]*\)#.vsh\1#')
  output_script_path="$par_output_dir/$output_script_relative"
  
  # writing header
  echo "> Writing script with header to $output_script_relative"
  yq delete "$par_input" 'functionality.resources.[0]' | sed "s/^/#' /" > "$output_script_path"
  
  # writing script
  awk "/VIASH START/,/VIASH END/ { next; }; 1 {print; }" "$input_script_path" >> "$output_script_path"

# ------------------------ CONFIG -> SPLIT ------------------------
elif [ $input_type = "config" ] && [ $par_format = "split" ]; then
  echo "Converting from 'config' to 'split'"

  # determine output paths
  funcionality_yaml_relative="functionality.yaml"
  funcionality_yaml_path="$par_output_dir/$funcionality_yaml_relative"
  
  # WRITING FUNCTIONALITY YAML
  echo "> Writing functionality yaml to $funcionality_yaml_relative"
  yq r "$par_input" functionality > "$funcionality_yaml_path"
  
  #### PLATFORM(S)
  # create platform yamls
  platforms=$(yq read "$par_input" platforms.*.type)
  for plat in $platforms; do
    platform_yaml_relative="platform_${plat}.yaml"
    platform_yaml_path="$par_output_dir/$platform_yaml_relative"
    echo "> Writing platform yaml to $platform_yaml_relative"
    yq read "$par_input" platforms.[type==$plat] > "$platform_yaml_path"
  done
  
  # copy script
  input_script_relative=$(yq read "$par_input" 'functionality.resources.[0].path')
  input_script_path="$input_dir/$input_script_relative"
  output_script_path="$par_output_dir/$input_script_relative"
  
  if [ "$input_script_path" != "$output_script_path" ]; then
    cp "$input_script_path" "$output_script_path"
  fi

fi




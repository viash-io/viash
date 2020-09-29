#' functionality:
#'   name: vshtrafo
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
#'     description: Output format. Must be one of 'script', 'config', 'split'
#'     required: true
#'   - name: "--rm"
#'     type: boolean_true
#'     description: Remove the source files after use.
#'   tests:
#'   - type: bash_script
#'     path: tests/test_conversion.sh
#'   - path: tests/functionality.yaml
#'   - path: tests/config.vsh.yaml
#'   - path: tests/platform_docker.yaml
#'   - path: tests/platform_native.yaml
#'   - path: tests/script.sh
#'   - path: tests/script.vsh.sh
#' platforms:
#' - type: docker
#'   image: mikefarah/yq
#'   apk: 
#'     packages:
#'     - bash
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
elif [[ "$par_input" =~ ^.*\.(yaml|yml)$ ]]; then
  input_type=split
else
  echo Input: unsupported format.
  exit 1
fi

# create dir if it does not exist
[[ -d "$par_output_dir" ]] || mkdir -p "$par_output_dir"

# format backwards compatibility
if [[ $par_format == "joined" ]]; then
  echo "Output: deprecated format 'joined' detected. Use 'config' instead."
  par_format="config"
fi

# check format
if [[ ! $par_format =~ ^(script|config|split)$ ]]; then
  echo "Output: unsupported format. Must be one of 'script', 'config' or 'split'"
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
    echo "$CONFIG_YAML" | yq read - functionality.resources | yq p - functionality.resources | yq m -a "$config_yaml_path" - -i
  fi

  # WRITING SCRIPT
  echo "> Writing script to $output_script_relative"
  cat "$par_input" | grep -v "^#' " > "$output_script_path"
  
# ------------------------ SCRIPT -> SPLIT ------------------------
elif [ $input_type = "script" ] && [ $par_format = "split" ]; then
  echo "Converting from 'script' to 'split'"

  # determine output paths
  funcionality_yaml_relative="functionality.yaml"
  funcionality_yaml_path="$par_output_dir/$funcionality_yaml_relative"
  output_script_relative="$(basename $par_input | sed 's#\.vsh\.#.#')"
  output_script_path="$par_output_dir/$output_script_relative"
  
  # WRITING FUNCTIONALITY YAML
  echo "> Writing functionality yaml to $funcionality_yaml_relative"
  FUNCTIONALITY_YAML=$(cat "$par_input" | grep "^#' " | sed "s/^#' //" | yq read - functionality)
  
  # write yaml without resources
  echo "$FUNCTIONALITY_YAML" | yq d - resources > "$funcionality_yaml_path"
  
  # add script to resources
  printf "resources:\n- type: ${script_type}_script\n  path: $output_script_relative\n" | yq m "$funcionality_yaml_path" - -i
  
  # add other resources
  has_resources=`echo "$FUNCTIONALITY_YAML" | yq read - resources | head -1`
  if [ ! -z "$has_resources" ]; then
    echo "$FUNCTIONALITY_YAML" | yq read - resources | yq p - resources | yq m -a "$funcionality_yaml_path" - -i
  fi
  
  #### PLATFORM(S)
  # create platform yamls
  platforms=`cat "$par_input" | grep "^#' " | sed "s/^#' //" | yq read - platforms.*.type`
  for plat in $platforms; do
    platform_yaml_relative="platform_${plat}.yaml"
    platform_yaml_path="$par_output_dir/$platform_yaml_relative"
    echo "> Writing platform yaml to $platform_yaml_relative"
    cat "$par_input" | grep "^#' " | sed "s/^#' //" | yq read - platforms.[type==$plat] > "$platform_yaml_path"
  done

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
  cp "$input_script_path" "$output_script_path"

# ------------------------ SPLIT -> SCRIPT ------------------------
elif [ $input_type = "split" ] && [ $par_format = "script" ]; then
  echo "Converting from 'split' to 'script'"

  # determine output paths
  input_script_relative=$(yq read "$par_input" 'resources.[0].path')
  input_script_path="$input_dir/$input_script_relative"
  output_script_relative=$(echo "$input_script_relative" | sed 's#\(\.[^\.]*\)#.vsh\1#')
  output_script_path="$par_output_dir/$output_script_relative"
  
  echo "> Writing script with header to $output_script_relative"
  
  # writing functionality
  echo "#' functionality:" > "$output_script_path"
  yq delete "$par_input" 'resources.[0]' | sed "s/^/#'   /" >> "$output_script_path"
  
  # writing platforms
  possible_platforms="native docker nextflow"
  
  # check if any platforms exist
  found_platforms=false
  for plat in $possible_platforms; do
    platform_relative="platform_$plat.yaml"
    platform_path="$input_dir/$platform_relative"
    if [ -f "$platform_path" ]; then
      found_platforms=true
    fi
  done
  
  if [ "$found_platforms" = "false" ]; then
    echo "#' platforms: []" >> "$output_script_path"
  else 
    echo "#' platforms:" >> "$output_script_path"
    
    for plat in $possible_platforms; do
      platform_relative="platform_$plat.yaml"
      platform_path="$input_dir/$platform_relative"
      if [ -f "$platform_path" ]; then
        yq delete "$platform_path" 'volumes' | sed "s/^/#'   /" | sed "s/#'   type:/#' - type:/" >> "$output_script_path" # should not assume type is first!  
      fi
    done
  fi
  
  # writing script
  awk "/VIASH START/,/VIASH END/ { next; }; 1 {print; }" "$input_script_path" >> "$output_script_path"
  
# ------------------------ SPLIT -> CONFIG ------------------------
elif [ $input_type = "split" ] && [ $par_format = "config" ]; then
  echo "Converting from 'split' to 'config'"

  # determine output paths
  output_yaml_relative="config.vsh.yaml"
  output_yaml_path="$par_output_dir/$output_yaml_relative"
  
  # WRITING CONFIG YAML
  echo "> Writing config yaml to $output_yaml_relative"
  
  # writing functionality
  yq p "$par_input" functionality > "$output_yaml_path"
  
  # writing platforms  
  possible_platforms="native docker nextflow"
  
  for plat in $possible_platforms; do
    platform_relative="platform_$plat.yaml"
    platform_path="$input_dir/$platform_relative"
    if [ -f "$platform_path" ]; then
      yq p "$platform_path" platforms[+] | yq m -i -a "$output_yaml_path" - 
    fi
  done
  
  # copy script
  input_script_relative=$(yq read "$par_input" 'resources.[0].path')
  input_script_path="$input_dir/$input_script_relative"
  output_script_path="$par_output_dir/$input_script_relative"
  cp "$input_script_path" "$output_script_path"

fi




function ViashParseArgumentValue {
  local env_name="$1"
  local multiple="$2"
  local flag="$3"
  local value="$4"

  if [ $# -lt 4 ]; then
    ViashError "Not enough arguments passed to ${flag}. Use '--help' to get more information on the parameters."
    exit 1
  fi

  if [ "$multiple" == "false" ]; then
    # check whether the variable is already set
    if [ ! -z ${!env_name+x} ]; then
      local -n prev_value="$env_name"
      ViashError "Pass only one argument to argument '${flag}'. Found: ${prev_value@Q} & ${value@Q}"
      exit 1
    fi

    # set the variable
    declare -g "$env_name=${value}"
  else
    local new_values

    local -n prev_values="$env_name"

    # todo: allow escaping the delimiter
    readarray -d ';' -t new_values < <(printf '%s' "$value")

    combined_values=( "${prev_values[@]}" "${new_values[@]}" )

    declare -g -a "$env_name=(\"\${combined_values[@]}\")"
  fi
}

# ViashParseArgumentValue: Parse the value of an argument
# 
# This script is used by to parse the value of an argument and set
# the corresponding environment variable.
# 
# If the argument is multiple: false:
# 
# * If the variable is already set, an error is thrown.
# * If the value is equal to UNDEFINED, it is replaced with
#   @@VIASH_UNDEFINED@@.
# * If the value is quoted, the quotes are removed.
#
# If the argument is multiple: true:
#
# * If the value is equal to UNDEFINED, it is replaced with
#   @@VIASH_UNDEFINED@@.
# * If the value is quoted, the quotes are removed.
# * If the value is a list of values, the values are split by semicolons.
# * If the value contains escaped characters '"\;, they are unescaped.
#
# Arguments:
# $1: The name of an argument (used for error messages)
# $2: The name of the environment variable to set
# $3: Whether the argument can be passed multiple times (true/false)
# $4: The value of the argument
# return: None, but sets the environment variable
# 
# Examples:
#
# ViashParseArgumentValue "--input" "par_input" "true" "'UNDEFINED'" "false"
#
# See ViashParseArgumentValue.test.sh for additional examples.
#
# Note: This function is written to be compatible with bash 3.2 (macOS default)
# and avoids bash 4+ features like declare -g, local -n, ${var@Q}, and readarray.
function ViashParseArgumentValue {
  local flag="$1"
  local env_name="$2"
  local multiple="$3"
  local value="$4"

  if [ $# -lt 4 ]; then
    ViashError "Not enough arguments passed to ${flag}. Use '--help' to get more information on the parameters."
    exit 1
  fi

  if [ "$multiple" == "false" ]; then
    # check whether the variable is already set (bash 3.2 compatible)
    eval "local is_set=\${${env_name}+x}"
    if [ ! -z "$is_set" ]; then
      eval "local prev_value=\"\${$env_name}\""
      ViashError "Pass only one argument to argument '${flag}'. Found: '${prev_value}' & '${value}'"
      exit 1
    fi

    value=$(ViashParseSingleString "$value")

    # set the variable globally (bash 3.2 compatible - using eval instead of declare -g)
    # use printf %q to safely escape the value for eval to prevent backtick/command substitution
    local escaped_value
    escaped_value=$(printf '%q' "$value")
    eval "$env_name=$escaped_value"
  else
    # Get existing array values (bash 3.2 compatible)
    eval "local prev_values=(\"\${${env_name}[@]}\")"

    # Parse new values into array (bash 3.2 compatible - using while loop instead of readarray)
    local new_values=()
    while IFS= read -r line || [ -n "$line" ]; do
      new_values+=("$line")
    done < <(ViashParseMultipleStringAsArray "$value")

    local combined_values=( "${prev_values[@]}" "${new_values[@]}" )

    # if length is larger than 1 and some element is @@VIASH_UNDEFINED@@, throw error
    if [ ${#combined_values[@]} -gt 1 ]; then
      for element in "${combined_values[@]}"; do
        if [ "$element" == "@@VIASH_UNDEFINED@@" ]; then
          # bash 3.2 compatible quoting (using printf %q instead of ${var@Q})
          local combined_quoted=$(printf '%q ' "${combined_values[@]}")
          ViashError "Argument '${flag}': If argument value 'UNDEFINED' is passed, no other values should be provided.\nFound: ${combined_quoted}"
          exit 1
        fi
      done
    fi

    # Set the global array (bash 3.2 compatible - using eval instead of declare -g -a)
    eval "$env_name=(\"\${combined_values[@]}\")"
  fi
}

function ViashParseSingleString() {
  local value="$1"

  # if value is equal to UNDEFINED, replace with @@VIASH_UNDEFINED@@
  if [ "$value" == "UNDEFINED" ]; then
    value="@@VIASH_UNDEFINED@@"
  fi

  # if value is quoted, remove the quotes
  if [[ "$value" =~ ^\".*\"$ ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "$value" =~ ^\'.*\'$ ]]; then
    value="${value:1:${#value}-2}"
  fi

  echo "$value"
}

function ViashParseMultipleStringAsArray() {
  local value="$1"

  # if value is equal to UNDEFINED, replace with @@VIASH_UNDEFINED@@
  if [ "$value" == "UNDEFINED" ]; then
    value="@@VIASH_UNDEFINED@@"
  fi

  # escape slashes
  value="${value//\\\\/@@VIASH_ESCAPE_SLASH@@}"
  # escape semicolons
  value="${value//\\;/@@VIASH_ESCAPE_SEMICOLON@@}"
  # escape quotes
  value="${value//\\\"/@@VIASH_ESCAPE_QUOTE@@}"
  # escape single quotes
  value="${value//\\\'/@@VIASH_ESCAPE_SINGLE_QUOTE@@}"

  local gawk_script="
    BEGIN {
      FPAT = \"([^;]+)|(\\\"[^\\\"]+\\\")|('[^']+')\";
    }

    {
      for (i = 1; i <= NF; i++) {
        field = \$i;

        # if field is equal to 'UNDEFINED_ITEM', replace it with @@VIASH_UNDEFINED_ITEM@@
        if (field == \"UNDEFINED_ITEM\") {
          field = \"@@VIASH_UNDEFINED_ITEM@@\";
        }

        # Remove leading and trailing double quotes.
        if (field ~ /^\".*\"$/) {
          gsub(/^\"|\"$/, \"\", field);
        } else if (field ~ /^'.*'$/) {
          gsub(/^'|'$/, \"\", field);
        }

        # Unescape escaped characters
        gsub(/@@VIASH_ESCAPE_SINGLE_QUOTE@@/, \"'\", field);
        gsub(/@@VIASH_ESCAPE_QUOTE@@/, \"\\\"\", field);
        gsub(/@@VIASH_ESCAPE_SEMICOLON@@/, \";\", field);
        gsub(/@@VIASH_ESCAPE_SLASH@@/, \"\\\\\", field);

        # Output each field on a separate line.
        print field;
      }
    }
  "

  echo "${value}" | awk "$gawk_script"
}
